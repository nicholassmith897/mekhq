package mekhq.gui.dialog;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.common.AmmoType;
import megamek.common.UnitType;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignFactory;
import mekhq.campaign.Kill;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.FileDialogs;

public class CampaignExportWizard extends JDialog {
    /**
     * 
     */
    private static final long serialVersionUID = -7171621116865584010L;
    
    private JList<Force> forceList;
    private JList<Person> personList;
    private JList<Unit> unitList;
    private JList<Part> partList;
    private JList<PartCount> partCountList;
    
    private JTextField txtPartCount = new JTextField();
    private JButton btnUpdatePartCount = new JButton();
    
    private JCheckBox chkExportState = new JCheckBox();
    private JCheckBox chkExportContractOffers = new JCheckBox();
    private JCheckBox chkExportCompletedContracts = new JCheckBox();
    private JCheckBox chkDestructiveExport = new JCheckBox();
    private JTextField txtExportMoney = new JTextField();
    private JLabel lblMoney = new JLabel();
    private JLabel lblStatus;
    private ResourceBundle resourceMap;
    
    private Campaign sourceCampaign;
    private Campaign destinationCampaign;
    
    private Optional<File> destinationCampaignFile;
    
    public enum CampaignExportWizardState {
        ForceSelection,
        PersonSelection,
        UnitSelection,
        PartSelection,
        PartCountSelection,
        MiscellaneousSelection,
        DestinationFileSelection // this should always be last
    }
    
    public CampaignExportWizard(Campaign c) {
        resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignExportWizard", new EncodeControl());
        chkExportState.setText(resourceMap.getString("chkExportSettings.text"));
        chkExportState.setToolTipText(resourceMap.getString("chkExportSettings.tooltip"));
        chkExportContractOffers.setText(resourceMap.getString("chkExportContractOffers.text"));
        chkExportCompletedContracts.setText(resourceMap.getString("chkExportCompletedContracts.text"));
        lblMoney.setText(resourceMap.getString("lblMoney.text"));
        chkDestructiveExport.setText(resourceMap.getString("chkDestructiveExport.text"));        
        
        sourceCampaign = c;
        setupForceList();
        setupPersonList();
        setupUnitList();
        setupPartList();
        chkDestructiveExport.setToolTipText(resourceMap.getString("chkDestructiveExport.tooltip"));
        btnUpdatePartCount.setText(resourceMap.getString("btnUpdatePartCount.text"));
    }
    
    public void display(CampaignExportWizardState state) {
        getContentPane().removeAll();
        getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        JLabel lblInstructions = new JLabel();
        getContentPane().add(lblInstructions, gbc);
        
        gbc.gridy++;
        
        lblStatus = new JLabel();
        getContentPane().add(lblStatus, gbc);
        
        gbc.gridy++;
        
        JScrollPane scrollPane = new JScrollPane();
        switch(state) {
        case ForceSelection:
            lblInstructions.setText(resourceMap.getString("lblInstructions.ForceSelection.text"));
            scrollPane.setViewportView(forceList);
            getContentPane().add(scrollPane, gbc);
            break;
        case PersonSelection:
            lblInstructions.setText(resourceMap.getString("lblInstructions.PersonSelection.text"));
            lblStatus.setText(getPersonSelectionStatus());
            scrollPane.setViewportView(personList);
            getContentPane().add(scrollPane, gbc);
            break;
        case UnitSelection:
            lblInstructions.setText(resourceMap.getString("lblInstructions.UnitSelection.text"));
            lblStatus.setText(getUnitSelectionStatus());
            scrollPane.setViewportView(unitList);
            getContentPane().add(scrollPane, gbc);
            break;
        case PartSelection:
            lblInstructions.setText(resourceMap.getString("lblInstructions.PartSelection.text"));
            scrollPane.setViewportView(partList);
            getContentPane().add(scrollPane, gbc);
            break;
        case PartCountSelection:
            lblInstructions.setText(resourceMap.getString("lblInstructions.PartCountSelection.text"));
            setupPartCountList();
            scrollPane.setViewportView(partCountList);
            getContentPane().add(scrollPane, gbc);
            
            gbc.gridx++;
            txtPartCount.setText("0");
            txtPartCount.setColumns(5);
            gbc.insets = new Insets(1, 1, 1, 1);
            getContentPane().add(txtPartCount, gbc);
            
            gbc.gridx++;
            getContentPane().add(btnUpdatePartCount, gbc);
            gbc.gridx -= 2;
            
            lblStatus.setText(getPartCountSelectionStatus());
            break;
        case MiscellaneousSelection:
            lblInstructions.setText(resourceMap.getString("lblInstructions.MiscSelection.text"));
            gbc.anchor = GridBagConstraints.WEST;
            getContentPane().add(chkExportState, gbc);
            gbc.gridy++;
            getContentPane().add(chkExportContractOffers, gbc);
            gbc.gridy++;
            getContentPane().add(chkExportCompletedContracts, gbc);
            gbc.gridy++;
                        
            JPanel pnlMoney = new JPanel();
            pnlMoney.setLayout(new GridBagLayout());
            GridBagConstraints mgbc = new GridBagConstraints();
            mgbc.fill = GridBagConstraints.REMAINDER;
            mgbc.insets = new Insets(1, 1, 1, 1);
            mgbc.gridy = 0;
            mgbc.gridx = 0;
            
            txtExportMoney.setText("0");
            txtExportMoney.setColumns(5);
            pnlMoney.add(txtExportMoney, mgbc);
            mgbc.gridx++;
            pnlMoney.add(lblMoney, mgbc);
            getContentPane().add(pnlMoney, gbc);
            
            gbc.gridy++;
            getContentPane().add(chkDestructiveExport, gbc);
            break;
        case DestinationFileSelection:
            lblInstructions.setText(resourceMap.getString("lblInstructions.Finalize.text"));
            JButton btnNewCampaign = new JButton(resourceMap.getString("btnNewCampaign.text"));
            btnNewCampaign.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    destinationCampaignFile = FileDialogs.saveCampaign(null, sourceCampaign);
                    if(destinationCampaignFile.isPresent()) {
                        exportToCampaign(destinationCampaignFile.get());
                        setVisible(false);
                    }
                }
            });
            getContentPane().add(btnNewCampaign, gbc);
            gbc.gridx++;
            
            JButton btnExistingCampaign = new JButton(resourceMap.getString("btnExistingCampaign.text"));
            btnExistingCampaign.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    destinationCampaignFile = FileDialogs.openCampaign(null);
                    if(destinationCampaignFile.isPresent()) {
                        exportToCampaign(destinationCampaignFile.get());
                        setVisible(false);
                    }
                }
            });
            getContentPane().add(btnExistingCampaign, gbc);
            gbc.gridx--;
        }
        
        gbc.gridy++;
        
        if(state != CampaignExportWizardState.DestinationFileSelection) {
            JButton btnNext = new JButton(resourceMap.getString("btnNext.text"));
            btnNext.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    nextButtonHandler(state);
                }
            });
            
            getContentPane().add(btnNext, gbc);
        }
        
        validate();
        pack();
        setLocationRelativeTo(getParent());
        setModalityType(ModalityType.APPLICATION_MODAL);
        setVisible(true);
    }
    
    private void setupForceList() {
        forceList = new JList<>();
        DefaultListModel<Force> forceListModel = new DefaultListModel<>();
        for(Force force : sourceCampaign.getAllForces()) {
            forceListModel.addElement(force);
        }
        forceList.setModel(forceListModel);
        forceList.setCellRenderer(new ForceListCellRenderer());
    }
    
    private void setupPersonList() {
        personList = new JList<>();
        DefaultListModel<Person> personListModel = new DefaultListModel<>();
        for(Person person : sourceCampaign.getActivePersonnel()) {
            personListModel.addElement(person);
        }
        personList.setModel(personListModel);
        personList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                lblStatus.setText(getPersonSelectionStatus());
                pack();
            }
        });
        personList.setCellRenderer(new PersonListCellRenderer());
    }
    
    private void setupUnitList() {
        unitList = new JList<>();
        DefaultListModel<Unit> unitListModel = new DefaultListModel<>();
        for(Unit unit : sourceCampaign.getUnits()) {
            unitListModel.addElement(unit);
        }
        unitList.setModel(unitListModel);
        unitList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                lblStatus.setText(getUnitSelectionStatus());
                pack();
            }
        });
        unitList.setCellRenderer(new UnitListCellRenderer());
    }
    
    private void setupPartList() {
        partList = new JList<>();
        DefaultListModel<Part> partListModel = new DefaultListModel<>();
        List<Part> parts = sourceCampaign.getSpareParts();
        parts.sort(new Comparator<Part>() {
            @Override
            public int compare(Part o1, Part o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        
        for(Part part : parts) {
            // if the part isn't part of some other activity
            if(!part.isReservedForRefit() &&
                    !part.isReservedForReplacement() &&
                    !part.isBeingWorkedOn() &&
                    part.isPresent()) {
                partListModel.addElement(part);
            }
        }
        partList.setModel(partListModel);    
    }
    
    private void setupPartCountList() {
        partCountList = new JList<>();
        partCountList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultListModel<PartCount> partCountListModel = new DefaultListModel<>();
        for(Part part : partList.getSelectedValuesList()) {
            partCountListModel.addElement(new PartCount(part));
        }
        
        partCountList.setModel(partCountListModel);
        
        partCountList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                txtPartCount.setText(Integer.toString(partCountList.getSelectedValue().count));
            } 
        });
        
        btnUpdatePartCount.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int updatedPartCount = Integer.parseInt(txtPartCount.getText());
                    
                    PartCount partCount = partCountList.getSelectedValue();
                    if(updatedPartCount > 0 && updatedPartCount <= partCount.getMaxPartCount()) {
                        partCountList.getModel().getElementAt(partCountList.getSelectedIndex()).count = updatedPartCount;
                        partCountList.updateUI();
                        lblStatus.setText(getPartCountSelectionStatus());
                        pack();
                    }
                } catch(Exception exception) {
                    lblStatus.setText(resourceMap.getString("lblStatus.Error.text"));
                }
            }
            
        });
    }
    
    /**
     * updates the person list based on changes to the force selection list
     * and unit selection list
     */
    private void updatePersonList() {
        List<Integer> selectedIndices = Arrays.stream(personList.getSelectedIndices())
                .boxed()
                .collect(Collectors.toList()); 
        
        for(Force force : forceList.getSelectedValuesList()) {
            for(UUID unitID : force.getAllUnits()) {
                Unit unit = sourceCampaign.getUnit(unitID);
                
                for(Person person : unit.getActiveCrew()) {
                    // this approach recurs throughout the class, and I
                    // couldn't find any better way to select multiple items in a JList
                    personList.setSelectedValue(person, false);
                    selectedIndices.add(personList.getSelectedIndex());
                }
                
                if(unit.getTech() != null) {
                    personList.setSelectedValue(unit.getTech(), false);
                    selectedIndices.add(personList.getSelectedIndex());
                }
            }
            
            if(force.getTechID() != null) {
                personList.setSelectedValue(sourceCampaign.getPerson(force.getTechID()), false);
                selectedIndices.add(personList.getSelectedIndex());
            }
        }
        
        for(Unit unit : unitList.getSelectedValuesList()) { 
            for(Person person : unit.getActiveCrew()) {
                personList.setSelectedValue(person, false);
                selectedIndices.add(personList.getSelectedIndex());
            }
            
            if(unit.getTech() != null) {
                personList.setSelectedValue(unit.getTech(), false);
                selectedIndices.add(personList.getSelectedIndex());
            }
        }
        
        // somewhat awkward syntax but the person list expects an int array
        // and all we have is a list
        personList.setSelectedIndices(selectedIndices.stream().mapToInt(i->i).toArray());
    }
    
    /**
     * updates the unit list based on changes to the force selection list
     * and person selection list, without losing existing selections
     */
    private void updateUnitList() {
        List<Integer> selectedIndices = Arrays.stream(unitList.getSelectedIndices())
                .boxed()
                .collect(Collectors.toList()); 
        
        for(Force force : forceList.getSelectedValuesList()) {
            for(UUID unitID : force.getAllUnits()) {
                Unit unit = sourceCampaign.getUnit(unitID);
                
                unitList.setSelectedValue(unit, false);
                selectedIndices.add(unitList.getSelectedIndex());
            }
        }
        
        for(Person person : personList.getSelectedValuesList()) { 
            if(person.getUnitId() != null) {
                Unit unit = sourceCampaign.getUnit(person.getUnitId());
                
                unitList.setSelectedValue(unit, false);
                selectedIndices.add(unitList.getSelectedIndex());
            }
        }
        
        // somewhat awkward syntax but the person list expects an int array
        // and all we have is a list
        unitList.setSelectedIndices(selectedIndices.stream().mapToInt(i->i).toArray());
    }
    
    private void nextButtonHandler(CampaignExportWizardState state) {
        switch(state) {
        case ForceSelection:
            updatePersonList();
            updateUnitList();
            break;
        case PersonSelection:
            updateUnitList();
            break;
        case UnitSelection:
            updatePersonList();
            break;
        }
        
        display(CampaignExportWizardState.values()[state.ordinal() + 1]);
    }
    
    /**
     * Carry out the campaign export.
     * @param file Destination file.
     * @return Whether or not the operation succeeded.
     */
    private boolean exportToCampaign(File file) {
        boolean newCampaign = !file.exists();
        
        if(newCampaign) {
            destinationCampaign = new Campaign();
            destinationCampaign.setApp(sourceCampaign.getApp());
        } else {
            try {
                FileInputStream fis = new FileInputStream(file);
                destinationCampaign = CampaignFactory.newInstance(sourceCampaign.getApp()).createCampaign(fis);
                // Restores all transient attributes from serialized objects
                destinationCampaign.restore();
                destinationCampaign.cleanUp();
                fis.close();
            } catch (NullEntityException ex) {
                MekHQ.getLogger().error(this.getClass(), "exportToCampaign", 
                        "The following units could not be loaded by the campaign:\n" + ex.getError() + "\n\nPlease be sure to copy over any custom units before starting a new version of MekHQ.\nIf you believe the units listed are not customs, then try deleting the file data/mechfiles/units.cache and restarting MekHQ.\nIt is also possible that unit chassi and model names have changed across versions of MegaMek. You can check this by\nopening up MegaMek and searching for the units. Chassis and models can be edited in your MekHQ save file with a text editor.");
                return false;
            } catch (Exception ex) {
                MekHQ.getLogger().error(this.getClass(), "exportToCampaign", 
                        "The campaign file could not be loaded.\nPlease check the log file for details.");
                return false;
            } catch(OutOfMemoryError e) {
                MekHQ.getLogger().error(this.getClass(), "exportToCampaign", 
                        "MekHQ ran out of memory attempting to load the campaign file. \nTry increasing the memory allocated to MekHQ and reloading.\nSee the FAQ at http://megamek.org for details.");
                return false;
            }
        }
        
        if(chkExportState.isSelected()) {
            destinationCampaign.setFactionCode(sourceCampaign.getFactionCode());
            destinationCampaign.setCamoCategory(sourceCampaign.getCamoCategory());
            destinationCampaign.setCamoFileName(sourceCampaign.getCamoFileName());
            destinationCampaign.getCalendar().setTime(sourceCampaign.getDate());
            destinationCampaign.setLocation(sourceCampaign.getLocation());
        }
        
        if(chkExportContractOffers.isSelected()) {
            for(Contract contract : sourceCampaign.getContractMarket().getContracts()) {
                destinationCampaign.getContractMarket().getContracts().add(contract);
            }
        }
        
        if(chkExportCompletedContracts.isSelected()) {
            for(Mission mission : sourceCampaign.getMissions()) {
                if(!mission.isActive()) {
                    destinationCampaign.importMission(mission);
                }
            }
        }
        
        int money = 0;
        
        try {
            money = Integer.parseInt(txtExportMoney.getText());
            destinationCampaign.addFunds(Money.of(money), String.format("Transfer from %s", sourceCampaign.getName()), Transaction.C_START);
        } catch(Exception e) {
            
        }
        
        // forces aren't moved/copied over, we just use the force selection to pre-populate the list of people and units 
        // to be exported
        
        for(Unit unit : unitList.getSelectedValuesList()) {
            if(destinationCampaign.getUnit(unit.getId()) != null) {
                destinationCampaign.removeUnit(unit.getId());
            }
            
            destinationCampaign.importUnit(unit);
            destinationCampaign.getUnit(unit.getId()).setForceId(Force.FORCE_NONE);
        }
        
        // overwrite any people with the same ID.
        for(Person person : personList.getSelectedValuesList()) {
            if(destinationCampaign.getPerson(person.getId()) != null) {
                destinationCampaign.removePerson(person.getId());
            }
            
            destinationCampaign.importPerson(person);

            for(Kill kill : sourceCampaign.getKillsFor(person.getId())) {
                destinationCampaign.importKill(kill);
            }
        }
        
        // there's just no way to overwrite parts
        // so we simply add them to the destination
        for(int partcIndex = 0; partcIndex < partCountList.getModel().getSize(); partcIndex++) {
            PartCount partCount = partCountList.getModel().getElementAt(partcIndex);
            
            // make a copy of the part so we don't mess with the existing part
            // ammo and armor require special handling
            Part newPart = partCount.part.clone();
            if(newPart instanceof AmmoStorage) {
                ((AmmoStorage) newPart).setShots(partCount.count);
                destinationCampaign.addPart(newPart, 0);
            } else if (newPart instanceof Armor) {
                ((Armor) newPart).setAmount(partCount.count);
                destinationCampaign.addPart(newPart, 0);
            } else {
                // addPart only increments count by one and folds the part
                // into an existing part, making it impossible to modify the part
                // once it's inside the destination campaign, so we just add the part repeatedly
                for(int x = 0; x < partCount.count; x++) {
                    destinationCampaign.addPart(newPart, 0);
                }
            }
        }
        
        boolean saved = CampaignGUI.saveCampaign(null, destinationCampaign, file);
        
        // having saved the destination campaign, we can now get rid of stuff in the source
        // campaign, if we're doing a destructive export
        // don't do it if we failed to save for some reason.
        if(saved && chkDestructiveExport.isSelected()) {
            for(Unit unit : unitList.getSelectedValuesList()) {            
                sourceCampaign.removeUnit(unit.getId());
            }
            
            for(Person person : personList.getSelectedValuesList()) {
                sourceCampaign.removePerson(person.getId(), true);
            }
            
            if(money > 0) {
                sourceCampaign.addFunds(Money.of(-money), "Transfer to exported campaign", Transaction.C_START);
            }
            
            // here, we update the quantity of the relevant part in the source campaign
            // and remove it if we reach 0. ammo and armor require special handling as usual.            
            for(int partcIndex = 0; partcIndex < partCountList.getModel().getSize(); partcIndex++) {
                PartCount partCount = partCountList.getModel().getElementAt(partcIndex);
                
                if(partCount.part instanceof AmmoStorage) {
                    AmmoStorage sourceAmmo = (AmmoStorage) partCount.part;
                    sourceAmmo.changeShots(-partCount.count);
                    
                    if(sourceAmmo.getShots() <= 0) {
                        sourceCampaign.removePart(partCount.part);
                    }
                } else if (partCount.part instanceof Armor) {
                    Armor sourceArmor = (Armor) partCount.part;
                    sourceArmor.setAmount(sourceArmor.getAmount() - partCount.count);
                    
                    if(sourceArmor.getAmount() <= 0) {
                        sourceCampaign.removePart(partCount.part);
                    }
                } else {
                    partCount.part.setQuantity(partCount.part.getQuantity() - partCount.count);
                    
                    if(partCount.part.getQuantity() <= 0) {
                        sourceCampaign.removePart(partCount.part);
                    }
                }
            }
        }

        return saved;
    }
    
    private String getPersonSelectionStatus() {
        Map<String, Integer> roleCounts = new HashMap<>();
        for(Person person : personList.getSelectedValuesList()) {
            if(!roleCounts.containsKey(person.getPrimaryRoleDesc())) {
                roleCounts.put(person.getPrimaryRoleDesc(), 0);
            }
            
            roleCounts.put(person.getPrimaryRoleDesc(), roleCounts.get(person.getPrimaryRoleDesc()) + 1);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        
        for(String key : roleCounts.keySet()) {
            sb.append(String.format("%s (%d)<br/>", key, roleCounts.get(key)));
        }
        
        sb.append("</html>");
        return sb.toString();
    }
    
    private String getUnitSelectionStatus() {
        Map<Integer, Integer> typeCounts = new HashMap<>();
        for(Unit unit : unitList.getSelectedValuesList()) {
            if(!typeCounts.containsKey(unit.getEntity().getUnitType())) {
                typeCounts.put(unit.getEntity().getUnitType(), 0);
            }
            
            typeCounts.put(unit.getEntity().getUnitType(), typeCounts.get(unit.getEntity().getUnitType()) + 1);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        
        for(Integer key : typeCounts.keySet()) {
            sb.append(String.format("%s (%d)<br/>", UnitType.getTypeName(key), typeCounts.get(key)));
        }
        
        sb.append("</html>");
        return sb.toString();
    }
    
    private String getPartCountSelectionStatus() {
        double totalTonnage = 0;
        for(int partIndex = 0; partIndex < partCountList.getModel().getSize(); partIndex++) {
            PartCount partCount = partCountList.getModel().getElementAt(partIndex);
            totalTonnage += partCount.getCurrentTonnage();
        }
        
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        return String.format("%s tons selected", nf.format(totalTonnage));
    }
    
    private class UnitListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            ((JLabel) cmp).setText(((Unit) value).getName());
            return cmp;
        }
    }
    
    private class PersonListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Person person = (Person) value;
            String cellValue = String.format("%s (%s)", person.getFullName(), person.getPrimaryRoleDesc());
            ((JLabel) cmp).setText(cellValue);
            return cmp;
        }
    }
    
    private class ForceListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Force force = (Force) value;
            String cellValue = force.getFullName();
            ((JLabel) cmp).setText(cellValue);
            return cmp;
        }
    }
    
    private class PartCount {
        Part part;
        int count;
        
        public PartCount(Part part) {
            this.part = part;
            
            if(part instanceof Armor) {
                this.count = ((Armor) part).getAmount();
            } else if (part instanceof AmmoStorage) {
                this.count = ((AmmoStorage) part).getShots();
            } else {
                this.count = part.getQuantity();
            }
        }
        
        public int getMaxPartCount() {
            if(part instanceof Armor) {
                return ((Armor) part).getAmount();
            } else if (part instanceof AmmoStorage) {
                return ((AmmoStorage) part).getShots();
            } else {
                return part.getQuantity();
            }
        }
        
        public double getCurrentTonnage() {
            if(part instanceof Armor) {
                return ((Armor) part).getArmorWeight(count);
            } else if (part instanceof AmmoStorage) {
                AmmoStorage ammoPart = (AmmoStorage) part;
                AmmoType ammoType = (AmmoType) ammoPart.getType();
                return ammoType.getKgPerShot() * count / 1000.0;
            } else {
                return count * part.getTonnage() * 1.0;
            }
        }
        
        @Override
        public String toString() {
            return String.format("%s (%d)", part.getPartName(), count);
        }
    }
}
