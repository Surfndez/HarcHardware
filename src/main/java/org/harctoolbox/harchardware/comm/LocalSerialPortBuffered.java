/*
Copyright (C) 2012,2013,2014 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.harchardware.comm;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ICommandLineDevice;

public final class LocalSerialPortBuffered extends LocalSerialPort implements ICommandLineDevice {

    public static final int defaultBaudRate = 9600;

    public static void main(String[] args) {
        ArrayList<String> names;
        try (LocalSerialPortBuffered port = new LocalSerialPortBuffered("/dev/ttyS0", 9600, 8, 1, Parity.NONE, FlowControl.NONE, 10000, true)) {
            names = getSerialPortNames(false);
            names.forEach((name) -> {
                System.out.println(name);
            });

            String cmd = "#POW\r";
            port.open();
            port.sendString(cmd);
            System.out.println(port.readString());

        } catch (NoSuchPortException | PortInUseException | UnsupportedCommOperationException | IOException | HarcHardwareException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private static String escapeCommandLine(String cmd) {
        return cmd.replace("\r", "\\r").replace("\n", "\\n");
    }

    private BufferedReader bufferedInStream;

    public LocalSerialPortBuffered(String portName, int baud, int length, int stopBits, Parity parity, FlowControl flowControl, int timeout, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        super(portName, baud, length, stopBits, parity, flowControl, timeout);
        this.verbose = verbose;
    }

    public LocalSerialPortBuffered(String portName, int baud, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, baud, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, Parity.NONE, FlowControl.NONE, 0, verbose);
    }

    public LocalSerialPortBuffered(String portName, int baud, int timeout, boolean verbose) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, baud, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, Parity.NONE, FlowControl.NONE, timeout, verbose);
    }

    public LocalSerialPortBuffered(String portName, int baudRate) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, Parity.NONE, FlowControl.NONE, 0, false);
    }

    public LocalSerialPortBuffered(String portName) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(portName, defaultBaudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, Parity.NONE, FlowControl.NONE, 0, false);
    }

    public LocalSerialPortBuffered(int portNumber) throws IOException, NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
        this(getSerialPortName(portNumber), defaultBaudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, Parity.NONE, FlowControl.NONE, 0, false);
    }

    @Override
    public void open() throws HarcHardwareException, IOException {
        super.open();
        bufferedInStream = new BufferedReader(new InputStreamReader(inStream, Charset.forName("US-ASCII")));
    }

    @Override
    public void sendString(String cmd) throws IOException {
        if (verbose)
            System.err.println("LocalSerialPortBuffered.sendString: Sent '" + escapeCommandLine(cmd) + "'.");
        sendBytes(cmd.getBytes(Charset.forName("US-ASCII")));
    }

    //*@Override
    public void sendBytes(byte[] data) throws IOException {
        outStream.write(data);
    }

    public void sendBytes(byte[] data, int offset, int length) throws IOException {
        outStream.write(data, offset, length);
    }

    public void sendByte(byte b) throws IOException {
        outStream.write(b);
    }

    @Override
    public String readString() throws IOException {
        return readString(false);
    }

    @Override
    public String readString(boolean wait) throws IOException {
        if (!(wait || bufferedInStream.ready()))
            return null;

        try {
            String result = bufferedInStream.readLine();
            if (verbose)
                System.err.println("LocalSerialPortBuffered.readString: received "
                        + (result != null ? ("\"" + result + "\"") : "<null>"));
            return result;
        } catch (IOException ex) {
            if (ex.getMessage().equals("Underlying input stream returned zero bytes")) { // RXTX....
                if (verbose)
                    System.err.println("LocalSerialPortBuffered.readString: TIMEOUT");
                return null;
            } else
                throw ex;
        }
    }

    @Override
    public boolean ready() throws IOException {
        return bufferedInStream.ready();
    }

    @Override
    public void setDebug(int debug) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
