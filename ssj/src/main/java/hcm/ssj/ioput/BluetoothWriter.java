/*
 * BluetoothWriter.java
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

package hcm.ssj.ioput;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;

import hcm.ssj.core.Consumer;
import hcm.ssj.core.Util;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Johnny on 05.03.2015.
 */
public class BluetoothWriter extends Consumer {

    public class Options {
        public String serverName = "SSJ_BLServer";
        public String serverAddr; //if this is a client
        public String connectionName = "SSJ"; //must match that of the peer
        public BluetoothConnection.Type connectionType = BluetoothConnection.Type.CLIENT;
    }

    public Options options = new Options();

    private BluetoothConnection _conn;
    private DataOutputStream _out;
    private byte[] _data;

    private boolean _connected = false;

    public BluetoothWriter() {
        _name = "SSJ_consumer_BluetoothWriter";
    }

    @Override
    public void enter(Stream[] stream_in) {
        try {
            switch(options.connectionType)
            {
                case SERVER:
                    _conn = new BluetoothServer(options.connectionName, options.serverName);
                    _conn.connect();
                    break;
                case CLIENT:
                    _conn = new BluetoothClient(options.connectionName, options.serverName, options.serverAddr);
                    _conn.connect();
                    break;
            }

            _out = new DataOutputStream(_conn.getSocket().getOutputStream());
        } catch (Exception e)
        {
            Log.e(super._name, "error in setting up connection "+ options.connectionName, e);
            return;
        }

        BluetoothDevice dev = _conn.getSocket().getRemoteDevice();

        _data = new byte[stream_in[0].tot];

        Log.i(_name, "connected to " + dev.getName() + " @ " + dev.getAddress());
        _connected = true;
    }

    protected void consume(Stream[] stream_in) {
        if (!_connected)
            return;

        try {
            Util.arraycopy(stream_in[0].ptr(), 0, _data, 0, _data.length);
            _out.write(_data);

        } catch (IOException e) {
            Log.w(_name, "failed sending data", e);
        }
    }

    public void flush(Stream[] stream_in) {
        _connected = false;

        try {
            _conn.disconnect();
        } catch (IOException e) {
            Log.e(_name, "failed closing connection", e);
        }
    }

    @Override
    public void forcekill() {

        try {
            _conn.disconnect();
        } catch (IOException e) {
            Log.e(_name, "failed closing connection", e);
        }

        super.forcekill();
    }
}
