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

package io.kamax.apps.hbox.vbox6_1.setting.console;

import io.kamax.apps.hbox.vbox6_1.manager.VBoxSessionManager;
import io.kamax.apps.hbox.vbox6_1.setting._MachineSettingAction;
import io.kamax.hbox.constant.MachineAttribute;
import io.kamax.hboxd.exception.machine.MachineLockingException;
import io.kamax.tools.setting.StringSetting;
import io.kamax.tools.setting._Setting;
import org.virtualbox_6_1.IMachine;
import org.virtualbox_6_1.ISession;
import org.virtualbox_6_1.LockType;

public class VrdePortSettingAction implements _MachineSettingAction {

    @Override
    public LockType getLockType() {
        return LockType.Shared;
    }

    @Override
    public String getSettingName() {
        return MachineAttribute.VrdePort.getId();
    }

    @Override
    public void set(IMachine machine, _Setting setting) {
        machine.getVRDEServer().setVRDEProperty("TCP/Ports", setting.getString());
    }

    @Override
    public _Setting get(IMachine machine) {
        // TODO improve by having a getConsole() in SessionManager and throw the appropriate exception
        try {
            ISession sess = VBoxSessionManager.get().lockAuto(machine.getId());
            try {
                if ((sess.getConsole() != null) && (sess.getConsole().getVRDEServerInfo() != null) && (sess.getConsole().getVRDEServerInfo().getPort() > 0)) {
                    return new StringSetting(MachineAttribute.VrdePort, Integer.toString(sess.getConsole().getVRDEServerInfo().getPort()));
                } else {
                    return new StringSetting(MachineAttribute.VrdePort, machine.getVRDEServer().getVRDEProperty("TCP/Ports"));
                }
            } finally {
                VBoxSessionManager.get().unlockAuto(machine.getId());
            }
        } catch (MachineLockingException e) {
            return new StringSetting(MachineAttribute.VrdePort, machine.getVRDEServer().getVRDEProperty("TCP/Ports"));
        }
    }

}
