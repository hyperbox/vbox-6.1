/*
 * Hyperbox - Virtual Infrastructure Manager
 * Copyright (C) 2018 Kamax Sarl
 *
 * https://apps.kamax.io/hyperbox
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.apps.hbox.vbox6_1.manager;

import io.kamax.apps.hbox.vbox6_1.VBox;
import io.kamax.apps.hbox.vbox6_1.setting.*;
import io.kamax.apps.hbox.vbox6_1.storage.VBoxMedium;
import io.kamax.apps.hbox.vbox6_1.storage.VBoxStorageController;
import io.kamax.apps.hbox.vbox6_1.vm.VBoxMachine;
import io.kamax.apps.hbox.vbox6_1.vm.VBoxNetworkInterface;
import io.kamax.apps.hbox.vbox6_1.vm.VBoxSnapshot;
import io.kamax.hbox.ClassManager;
import io.kamax.hbox.comm.io.factory.SettingIoFactory;
import io.kamax.hbox.constant.EntityType;
import io.kamax.hbox.constant.MachineAttribute;
import io.kamax.hbox.constant.NetworkInterfaceAttribute;
import io.kamax.hbox.constant.StorageControllerAttribute;
import io.kamax.hbox.data.Device;
import io.kamax.hbox.data.Machine;
import io.kamax.hbox.exception.ConfigurationException;
import io.kamax.hbox.exception.HyperboxException;
import io.kamax.hbox.utils.Settings;
import io.kamax.tools.logging.Logger;
import io.kamax.tools.setting._Setting;
import org.virtualbox_6_1.*;

import java.util.*;

public class VBoxSettingManager {

    // private static boolean loaded = false;
    private static Map<String, _MachineSettingAction> vmActions;
    private static Map<String, _NetworkInterfaceSettingAction> nicActions;
    private static Map<String, _StorageControllerSettingAction> sctActions;
    private static Map<String, _MediumSettingAction> mediumActions;
    private static Map<String, _SnapshotSettingAction> snapshotActions;

    private static ISession session = null;

    /*
    private static void lockAuto(String uuid, LockType lockType) {
       session = VbSessionManager.get().lockAuto(uuid, lockType);
    }
     */

    private static void lockAuto(String uuid) {
        session = VBoxSessionManager.get().lockAuto(uuid);
    }

    private static void unlockAuto(String uuid) {
        VBoxSessionManager.get().unlockAuto(uuid, true);
        session = null;
    }

    private static IMachine getVm(String uuid) {
        return VBoxSessionManager.get().getCurrent(uuid);
    }

    static {
        try {
            loadMachineActions();
            loadNicActions();
            loadStrCtrlActions();
            loadMediumActions();
            loadSnapshotActions();
        } catch (HyperboxException e) {
            Logger.exception(e);
        }
    }

    // //////////////////////////////////////////////////////////
    // Snapshot code
    // //////////////////////////////////////////////////////////
    private static void loadSnapshotActions() throws HyperboxException {
        snapshotActions = new HashMap<String, _SnapshotSettingAction>();

        Set<_SnapshotSettingAction> subTypes = ClassManager.getAllOrFail(_SnapshotSettingAction.class);
        for (_SnapshotSettingAction item : subTypes) {
            snapshotActions.put(item.getSettingName(), item);
            Logger.debug("Linking " + item.getSettingName() + " to " + item.getClass().getSimpleName());
        }
    }

    public static List<_Setting> list(VBoxSnapshot snap) {
        List<_Setting> settings = new ArrayList<_Setting>();
        ISnapshot rawSnap = getVm(snap.getMachineId()).findSnapshot(snap.getUuid());
        for (_SnapshotSettingAction action : VBoxSettingManager.snapshotActions.values()) {
            _Setting setting = action.get(rawSnap);
            if (setting == null) {
                Logger.debug("Got a null value for setting " + action.getSettingName() + " handled by " + action.getClass().getName()
                        + " and will be ignored");
            } else {
                settings.add(setting);
            }
        }
        return settings;
    }

    public static void set(VBoxSnapshot snap, List<_Setting> settings) {

        for (_Setting setting : settings) {
            if (!snapshotActions.containsKey(setting.getName())) {
                throw new ConfigurationException("No action defined for setting [" + setting.getName() + "]");
            }
        }

        lockAuto(snap.getMachineId());
        try {
            for (_Setting setting : settings) {
                Logger.debug("Applying Setting ID [" + setting.getName() + "]");
                snapshotActions.get(setting.getName()).set(session.getMachine().findSnapshot(snap.getUuid()), setting);
            }
        } catch (VBoxException e) {
            throw new HyperboxException(e.getMessage());
        } finally {
            unlockAuto(snap.getMachineId());
        }
    }

    public static _Setting get(VBoxSnapshot snap, Object name) {
        String uniqueId = Settings.getUniqueId(name);
        if (!snapshotActions.containsKey(uniqueId)) {
            throw new ConfigurationException("No action defined for setting [" + uniqueId + "]");
        }

        ISnapshot snapshot = getVm(snap.getMachineId()).findSnapshot(snap.getUuid());
        _Setting s = snapshotActions.get(uniqueId).get(snapshot);
        return s;
    }

    // //////////////////////////////////////////////////////////
    // Machine code
    // //////////////////////////////////////////////////////////
    private static void loadMachineActions() throws HyperboxException {
        vmActions = new HashMap<String, _MachineSettingAction>();

        Set<_MachineSettingAction> subTypes = ClassManager.getAllOrFail(_MachineSettingAction.class);
        for (_MachineSettingAction item : subTypes) {
            vmActions.put(item.getSettingName(), item);
        }
    }

    public static void apply(IMachine vm, Machine vmData) {

        List<_Setting> settings = SettingIoFactory.getListIo(vmData.listSettings());
        for (_Setting setting : settings) {
            vmActions.get(setting.getName()).set(vm, setting);
        }

        for (Device dev : vmData.listDevices(EntityType.DvdDrive.getId())) {
            String controllerType = dev.getSetting(StorageControllerAttribute.Type).getString();
            String controllerSubType = dev.getSetting(StorageControllerAttribute.SubType).getString();
            try {
                vm.getStorageControllerByName(controllerType);
            } catch (VBoxException e) {
                if (e.getMessage().toLowerCase().contains("0x80bb0001")) {
                    vm.addStorageController(controllerType, StorageBus.valueOf(controllerType));
                } else {
                    Logger.exception(e);
                }
            }
            vm.getStorageControllerByName(controllerType).setControllerType(StorageControllerType.valueOf(controllerSubType));
            vm.attachDeviceWithoutMedium(controllerType, 0, 0, DeviceType.DVD);
        }

        for (Device dev : vmData.listDevices(EntityType.DiskDrive.getId())) {
            String controllerType = dev.getSetting(StorageControllerAttribute.Type).getString();
            String controllerSubType = dev.getSetting(StorageControllerAttribute.SubType).getString();
            try {
                vm.getStorageControllerByName(controllerType);
            } catch (VBoxException e) {
                if (e.getMessage().toLowerCase().contains("0x80bb0001")) {
                    vm.addStorageController(controllerType, StorageBus.valueOf(controllerType));
                } else {
                    Logger.exception(e);
                }
            }
            vm.getStorageControllerByName(controllerType).setControllerType(StorageControllerType.valueOf(controllerSubType));
        }

        for (Device dev : vmData.listDevices(EntityType.Network.getId())) {
            Long maxNic = VBox.get().getSystemProperties().getMaxNetworkAdapters(vm.getChipsetType());
            NetworkAdapterType adapterType = NetworkAdapterType.valueOf(dev.getSetting(NetworkInterfaceAttribute.AdapterType).getString());
            for (long i = 0; maxNic > i; i++) {
                vm.getNetworkAdapter(i).setAdapterType((adapterType));
            }
        }
    }

    public static List<_Setting> list(VBoxMachine vm) {
        List<_Setting> settings = new ArrayList<_Setting>();
        IMachine machine = getVm(vm.getUuid());
        for (_MachineSettingAction action : VBoxSettingManager.vmActions.values()) {
            _Setting setting = action.get(machine);
            if (setting == null) {
                Logger.debug("Got a null value for setting " + action.getSettingName() + " handled by " + action.getClass().getName()
                        + " and will be ignored");
            } else {
                settings.add(setting);
            }
        }
        return settings;
    }

    public static void set(VBoxMachine vm, List<_Setting> settings) {

        for (_Setting setting : settings) {
            if (!vmActions.containsKey(setting.getName())) {
                throw new ConfigurationException("No action defined for setting [" + setting.getName() + "]");
            }
        }

        /*
        LockType lockType = LockType.Shared;
        for (_Setting setting : settings) {
           if ((vmActions.get(setting.getName()).getLockType() != null) && !vmActions.get(setting.getName()).getLockType().equals(lockType)) {
              lockType = LockType.Write;
              break;
           }
        }

        lockAuto(vm.getUuid(), lockType);
         */
        lockAuto(vm.getUuid());
        try {
            for (_Setting setting : settings) {
                Logger.debug("Applying Setting ID [" + setting.getName() + "]");
                vmActions.get(setting.getName()).set(session.getMachine(), setting);
            }
        } catch (VBoxException e) {
            throw new HyperboxException(e.getMessage());
        } finally {
            unlockAuto(vm.getUuid());
        }
    }

    public static _Setting get(VBoxMachine vm, Object name) {
        String uniqueId = Settings.getUniqueId(name);
        if (!VBoxSettingManager.vmActions.containsKey(uniqueId)) {
            throw new ConfigurationException("No action defined for setting [" + uniqueId + "]");
        }

        IMachine machine = getVm(vm.getUuid());
        _Setting s = VBoxSettingManager.vmActions.get(uniqueId).get(machine);

        return s;
    }

    public static _Setting get(VBoxMachine vm, MachineAttribute setting) {
        return get(vm, setting.toString());
    }

    // ////////////////////////////////////////////
    // Network Interface code
    // ////////////////////////////////////////////
    private static void loadNicActions() throws HyperboxException {
        nicActions = new HashMap<String, _NetworkInterfaceSettingAction>();

        // TODO replace with .getAtLeastOneOrFail()
        Set<_NetworkInterfaceSettingAction> subTypes = ClassManager.getAllOrFail(_NetworkInterfaceSettingAction.class);
        for (_NetworkInterfaceSettingAction item : subTypes) {
            nicActions.put(item.getSettingName(), item);
        }
    }

    public static List<_Setting> list(VBoxNetworkInterface nic) {
        List<_Setting> settings = new ArrayList<_Setting>();
        INetworkAdapter nicAdaptor = getVm(nic.getMachineUuid()).getNetworkAdapter(nic.getNicId());
        for (_NetworkInterfaceSettingAction action : nicActions.values()) {
            _Setting setting = action.get(nicAdaptor);
            if (setting == null) {
                Logger.debug("Got a null value for setting " + action.getSettingName() + " handled by " + action.getClass().getName()
                        + " and will be ignored");
            } else {
                settings.add(setting);
            }
        }
        return settings;
    }

    public static void set(VBoxNetworkInterface nic, List<_Setting> settings) {

        for (_Setting setting : settings) {
            if (!nicActions.containsKey(setting.getName())) {
                throw new ConfigurationException("No action defined for setting [" + setting.getName() + "]");
            }
        }

        /*
        LockType lockType = LockType.Shared;
        for (_Setting setting : settings) {
           if ((nicActions.get(setting.getName()).getLockType() != null) && !nicActions.get(setting.getName()).getLockType().equals(lockType)) {
              lockType = LockType.Write;
              break;
           }
        }

        lockAuto(nic.getMachineUuid(), lockType);
         */

        lockAuto(nic.getMachineUuid());
        try {
            for (_Setting setting : settings) {
                Logger.debug("Applying Setting ID [" + setting.getName() + "]");
                _NetworkInterfaceSettingAction settingAction = VBoxSettingManager.nicActions.get(setting.getName());
                settingAction.set(session.getMachine().getNetworkAdapter(nic.getNicId()), setting);
            }
        } catch (VBoxException e) {
            throw new HyperboxException(e.getMessage());
        } finally {
            unlockAuto(nic.getMachineUuid());
        }
    }

    public static _Setting get(VBoxNetworkInterface nic, Object name) {
        String uniqueId = Settings.getUniqueId(name);
        if (!nicActions.containsKey(uniqueId)) {
            Logger.debug("Possible values for Network Adapter :");
            for (String key : nicActions.keySet()) {
                Logger.debug(key);
            }
            throw new ConfigurationException("No action defined for setting [" + uniqueId + "]");
        }

        return nicActions.get(uniqueId).get(getVm(nic.getMachineUuid()).getNetworkAdapter(nic.getNicId()));
    }

    public static _Setting get(VBoxNetworkInterface nic, NetworkInterfaceAttribute setting) {
        return get(nic, setting.toString());
    }

    // ///////////////////////////////////////////////
    // Storage Controller
    // ///////////////////////////////////////////////
    private static void loadStrCtrlActions() throws HyperboxException {
        sctActions = new HashMap<String, _StorageControllerSettingAction>();

        Set<_StorageControllerSettingAction> subTypes = ClassManager.getAllOrFail(_StorageControllerSettingAction.class);
        for (_StorageControllerSettingAction item : subTypes) {
            sctActions.put(item.getSettingName(), item);
        }
    }

    public static List<_Setting> list(VBoxStorageController sct) {
        List<_Setting> settings = new ArrayList<_Setting>();
        IStorageController storageCtrl = getVm(sct.getMachineUuid()).getStorageControllerByName(sct.getName());
        for (_StorageControllerSettingAction action : sctActions.values()) {
            _Setting setting = action.get(storageCtrl);
            if (setting == null) {
                Logger.debug("Got a null value for setting " + action.getSettingName() + " handled by " + action.getClass().getName()
                        + " and will be ignored");
            } else {
                settings.add(setting);
            }
        }
        return settings;
    }

    public static void set(VBoxStorageController strCtl, List<_Setting> settings) {

        for (_Setting setting : settings) {
            if (!sctActions.containsKey(setting.getName())) {
                throw new ConfigurationException("No action defined for setting [" + setting.getName() + "]");
            }
        }

        /*
        LockType lockType = LockType.Shared;
        for (_Setting setting : settings) {
           if (!sctActions.get(setting.getName()).getLockType().equals(lockType)) {
              lockType = LockType.Write;
              break;
           }
        }

        lockAuto(strCtl.getMachineUuid(), lockType);
         */

        lockAuto(strCtl.getMachineUuid());
        try {
            for (_Setting setting : settings) {
                Logger.debug("Applying Setting ID [" + setting.getName() + "]");
                _StorageControllerSettingAction settingAction = sctActions.get(setting.getName());
                settingAction.set(session.getMachine().getStorageControllerByName(strCtl.getName()), setting);
            }
        } catch (VBoxException e) {
            Logger.exception(e);
            throw new HyperboxException(e.getMessage());
        } finally {
            unlockAuto(strCtl.getMachineUuid());
        }
    }

    public static _Setting get(VBoxStorageController sct, Object name) {
        String uniqueId = Settings.getUniqueId(name);
        if (!sctActions.containsKey(uniqueId)) {
            Logger.debug("Possible values for Storage Controller :");
            for (String key : sctActions.keySet()) {
                Logger.debug(key);
            }
            throw new ConfigurationException("No action defined for setting [" + uniqueId + "]");
        }

        return sctActions.get(uniqueId).get(getVm(sct.getMachineUuid()).getStorageControllerByName(sct.getName()));
    }

    public static _Setting get(VBoxStorageController sct, StorageControllerAttribute setting) {
        return get(sct, setting.toString());
    }

    // //////////////////////////////////////////////////////
    // Medium Settings
    // //////////////////////////////////////////////////////
    private static void loadMediumActions() throws HyperboxException {
        mediumActions = new HashMap<String, _MediumSettingAction>();

        Set<_MediumSettingAction> subTypes = ClassManager.getAllOrFail(_MediumSettingAction.class);
        for (_MediumSettingAction item : subTypes) {
            mediumActions.put(item.getSettingName(), item);
        }
    }

    public static List<_Setting> list(VBoxMedium medium) {
        List<_Setting> settings = new ArrayList<_Setting>();
        IMedium rawMedium = VBox.get().openMedium(medium.getUuid(), DeviceType.valueOf(medium.getDeviceType()), AccessMode.ReadOnly, false);
        for (_MediumSettingAction action : mediumActions.values()) {
            _Setting setting = action.get(rawMedium);
            if (setting == null) {
                Logger.debug("Got a null value for setting " + action.getSettingName() + " handled by " + action.getClass().getName()
                        + " and will be ignored");
            } else {
                settings.add(setting);
            }
        }
        return settings;
    }

    public static void set(VBoxMedium medium, List<_Setting> settings) {

        for (_Setting setting : settings) {
            if (!mediumActions.containsKey(setting.getName())) {
                throw new ConfigurationException("No action defined for setting [" + setting.getName() + "]");
            }
        }

        IMedium rawMedium = VBox.get().openMedium(medium.getLocation(), DeviceType.valueOf(medium.getDeviceType()), AccessMode.ReadWrite, false);
        try {
            for (_Setting setting : settings) {
                Logger.debug("Applying Setting ID [" + setting.getName() + "]");
                mediumActions.get(setting.getName()).set(rawMedium, setting);
            }
        } catch (VBoxException e) {
            throw new HyperboxException(e.getMessage());
        }
    }

    public static _Setting get(VBoxMedium medium, Object name) {
        String uniqueId = Settings.getUniqueId(name);
        if (!mediumActions.containsKey(uniqueId)) {
            Logger.debug("Possible values for Medium :");
            for (String key : mediumActions.keySet()) {
                Logger.debug(key);
            }
            throw new ConfigurationException("No action defined for setting [" + uniqueId + "]");
        }

        IMedium rawMedium = VBox.get().openMedium(medium.getLocation(), DeviceType.valueOf(medium.getDeviceType()), AccessMode.ReadOnly, false);
        try {
            return mediumActions.get(uniqueId).get(rawMedium);
        } catch (VBoxException e) {
            throw new HyperboxException(e.getMessage());
        }
    }

    public static _Setting get(VBoxMedium medium, Enum<?> setting) {
        return get(medium, setting.toString());
    }
}
