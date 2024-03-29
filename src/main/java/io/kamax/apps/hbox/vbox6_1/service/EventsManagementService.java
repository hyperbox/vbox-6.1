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

package io.kamax.apps.hbox.vbox6_1.service;

import io.kamax.apps.hbox.vbox6_1.ErrorInterpreter;
import io.kamax.apps.hbox.vbox6_1.VBox;
import io.kamax.apps.hbox.vbox6_1.factory.EventFactory;
import io.kamax.hbox.event._Event;
import io.kamax.hbox.hypervisor.vbox.utils.EventBusFactory;
import io.kamax.hboxd.event._EventManager;
import io.kamax.hboxd.service.SimpleLoopService;
import io.kamax.tools.logging.Logger;
import org.virtualbox_6_1.IEvent;
import org.virtualbox_6_1.IEventListener;
import org.virtualbox_6_1.VBoxEventType;
import org.virtualbox_6_1.VBoxException;

import java.util.Arrays;

/**
 * Recommended way to handle events is the passive implementation, which required polling to get new events.<br/>
 * This service will keep polling, transform events and feed them into hyperbox.
 *
 * @author max
 */
public final class EventsManagementService extends SimpleLoopService {

    private _EventManager evMgr;
    private IEventListener el;

    public EventsManagementService(_EventManager evMgr) {
        this.evMgr = evMgr;
    }

    @Override
    protected void beforeLooping() {
        setSleepingTime(0);

        el = VBox.get().getEventSource().createListener();
        VBox.get().getEventSource().registerListener(el, Arrays.asList(VBoxEventType.Any), false);
        Logger.debug("Virtualbox Event Manager Server started.");
    }

    @Override
    protected void afterLooping() {
        if (el != null) {
            try {
                VBox.get().getEventSource().unregisterListener(el);
            } catch (Throwable t) {
                Logger.debug("Exception when trying to unregister listener on event source: " + t.getMessage());
            }
            el = null;
        }
        Logger.debug("Virtualbox Event Manager Server stopped.");
    }

    @Override
    protected void doLoop() {
        try {
            VBox.getManager().waitForEvents(0); // Needed to clear the internal event queue, see https://www.virtualbox.org/ticket/13647
            IEvent rawEvent = VBox.get().getEventSource().getEvent(el, 1000);
            if (rawEvent != null) {
                Logger.debug("Got an event from Virtualbox: " + rawEvent.getClass().getName() + " - " + rawEvent.getType() + " - " + rawEvent);
                IEvent preciseRawEvent = EventFactory.getRaw(rawEvent);
                if (preciseRawEvent != null) {
                    Logger.debug("Event was processed to " + preciseRawEvent.getClass().getName() + " - " + preciseRawEvent.getType() + " - "
                            + preciseRawEvent);
                    EventBusFactory.post(preciseRawEvent);
                    _Event ev = EventFactory.get(preciseRawEvent);
                    if (ev != null) {
                        evMgr.post(ev);
                    }
                }
                VBox.get().getEventSource().eventProcessed(el, rawEvent);
            }
        } catch (VBoxException e) {
            throw ErrorInterpreter.transform(e);
        } catch (RuntimeException r) {
            if ((r.getMessage() != null) && r.getMessage().contains("Connection refused")) {
                Logger.error("Virtualbox broke the connection with us, stopping the service");
                Logger.exception(r);
                stop();
            } else {
                throw r;
            }
        } catch (Throwable t) {
            Logger.error("Unexpected error occured in the VBox Event Manager - " + t.getMessage());
            Logger.exception(t);
            stop();
        }
    }

    @Override
    public String getId() {
        return "vbox-4.3-EventMgmtSvc";
    }

}
