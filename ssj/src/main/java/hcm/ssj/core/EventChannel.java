/*
 * EventChannel.java
 * Copyright (c) 2015
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler
 * *****************************************************
 * This file is part of the Social Signal Interpretation for Java (SSJ) framework
 * developed at the Lab for Human Centered Multimedia of the University of Augsburg.
 *
 * SSJ has been inspired by the SSI (http://openssi.net) framework. SSJ is not a
 * one-to-one port of SSI to Java, it is an approximation. Nor does SSJ pretend
 * to offer SSI's comprehensive functionality and performance (this is java after all).
 * Nevertheless, SSJ borrows a lot of programming patterns from SSI.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package hcm.ssj.core;

import android.util.Log;

import java.util.LinkedList;

/**
 * Created by Johnny on 05.03.2015.
 */
public class EventChannel
{

    protected String _name = "SSJ_EventChannel";

    private LinkedList<Event> _events = new LinkedList<>();
    private int _event_id = 0;

    final private Object _lock = new Object();
    protected boolean _terminate = false;

    protected TheFramework _frame;

    EventChannel()
    {
        _frame = TheFramework.getFramework();
    }
    
    public void reset()
    {
        _terminate = false;
        _event_id = 0;
    }

    public Event getLastEvent(boolean peek, boolean blocking) {

        Event ev = null;

        synchronized (_lock) {
            while (!_terminate && _events.size() == 0)
            {
                if(blocking)
                {
                    try { _lock.wait(); }
                    catch (InterruptedException e) {}
                }
                else
                    return null;
            }

            if(_terminate)
                return null;

            ev = _events.getLast();

            if (!peek)
                _events.removeLast();
        }

        return ev;
    }

    public Event getEvent(int eventID, boolean blocking) {

        synchronized (_lock) {
            while (!_terminate && (_events.size() == 0 || eventID > _events.getLast().id))
            {
                if(blocking)
                {
                    try { _lock.wait(); }
                    catch (InterruptedException e) {}
                }
                else
                    return null;
            }

            if(_terminate)
                return null;

            if(eventID == _events.getFirst().id)
                return _events.getFirst();

            if(eventID < _events.getFirst().id)
            {
                Log.w(_name, "event "+ eventID +" no longer in queue");
                return _events.getFirst(); //if event is no longer in queue, return oldest event
            }

            //search for event
            for (Event ev : _events)
            {
                if (ev.id == eventID)
                    return ev;
            }
        }
        return null;
    }

    public void pushEvent(Event ev)
    {
        synchronized (_lock) {
            //give event a local-unique ID
            ev.id = _event_id++;

            _events.addLast(ev);

//                Log.d(_name, "E_" + ev.id + "_" + ev.sender + ": name = " +ev.name+  " state = " + ev.state.toString() + " time = " + ev.time + " dur = " + ev.dur + " msg = " + ev.msg);

            if(_events.size() > Cons.MAX_NUM_EVENTS_PER_CHANNEL)
                _events.removeFirst();

            _lock.notifyAll();
        }
    }

    public void pushEvent(String msg) {
        pushEvent(new Event(msg));
    }

    public void pushEvent(byte[] msg, int pos, int len) {
        pushEvent(new Event(msg, pos, len));
    }


    public void close()
    {
        Log.i(_name, "shutting down");

        _terminate = true;

        synchronized (_lock)
        {
            _lock.notifyAll();
        }

        Log.i(_name, "shut down complete");
    }
}
