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

package io.kamax.apps.hbox.vbox6_1.setting.snapshot;

import io.kamax.apps.hbox.vbox6_1.setting._SnapshotSettingAction;
import io.kamax.hbox.constant.SnapshotAttribute;
import io.kamax.tools.setting.StringSetting;
import io.kamax.tools.setting._Setting;
import org.virtualbox_6_1.ISnapshot;
import org.virtualbox_6_1.LockType;

public class SnapshotNameSettingAction implements _SnapshotSettingAction {

    @Override
    public LockType getLockType() {
        return LockType.Shared;
    }

    @Override
    public String getSettingName() {
        return SnapshotAttribute.Name.getId();
    }

    @Override
    public void set(ISnapshot snap, _Setting setting) {
        String name = setting.getString();
        snap.setName(name);
    }

    @Override
    public _Setting get(ISnapshot snap) {
        return new StringSetting(SnapshotAttribute.Name, snap.getName());
    }

}
