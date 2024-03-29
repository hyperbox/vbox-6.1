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

package io.kamax.apps.hbox.vbox6_1.factory.event;

import io.kamax.apps.hbox.vbox6_1.VBox;
import io.kamax.apps.hbox.vbox6_1.factory._PreciseEventFactory;
import io.kamax.hbox.event._Event;
import io.kamax.hboxd.event.machine.MachineSnapshotDataChangedEvent;
import io.kamax.hboxd.event.snapshot.SnapshotTakenEvent;
import org.virtualbox_6_1.IEvent;
import org.virtualbox_6_1.ISnapshotTakenEvent;
import org.virtualbox_6_1.VBoxEventType;

public class SnapshotTakenEventFactory implements _PreciseEventFactory {

    @Override
    public VBoxEventType getType() {
        return VBoxEventType.OnSnapshotTaken;
    }

    @Override
    public ISnapshotTakenEvent getRaw(IEvent vbEvent) {

        return ISnapshotTakenEvent.queryInterface(vbEvent);
    }

    @Override
    public _Event getEvent(IEvent vbEvent) {

        ISnapshotTakenEvent snapEv = (ISnapshotTakenEvent) vbEvent;

        // Generic event might be used due to Webservices bug, depending on revision - See Javadoc of HyperboxEvents.MachineSnapshotDataChange
        // This revision is only valid for 4.2 branch
        if (VBox.get().getRevision() >= 90983) {
            return new SnapshotTakenEvent(snapEv.getMachineId(), snapEv.getSnapshotId());
        } else {
            return new MachineSnapshotDataChangedEvent(snapEv.getMachineId());
        }
    }

}
