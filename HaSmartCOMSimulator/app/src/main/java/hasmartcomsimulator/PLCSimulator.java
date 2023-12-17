package hasmartcomsimulator;

import static hasmart.primaryInterface.PrimaryInterface.BAUD_RATE;
import static hasmart.primaryInterface.PrimaryInterface.NUM_DATA_BITS;
import static hasmart.primaryInterface.PrimaryInterface.NUM_STOP_BITS;
import static hasmart.primaryInterface.PrimaryInterface.PARITY;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import hasmart.constants.ResultCode;
import hasmart.constants.SerialCommandContants;
import hasmart.primaryInterface.CommandType;
import hasmart.primaryInterface.PLCIndex;
import hasmart.primaryInterface.SerialCommand;
import hasmart.primaryInterface.Utils;

public class PLCSimulator implements Runnable, SerialPortDataListener {
    private final SerialPort port;

    private Map<Integer,Double> registers = new HashMap<Integer,Double>(){};

    private boolean running = false;
    private final PLCIndex index;

    public PLCSimulator(PLCIndex index,String portName) {
        SerialPort tempPort = SerialPort.getCommPort(portName);
        this.port = tempPort;
        this.index = index;

        initMap();
    }

    private void initMap(){
        for (int i = 0; i < 650; i++) {
            if (i==600){
                if (index == PLCIndex.PLC1){
                    registers.put(i,int2DoubleInSameBits(10));
                }else {
                    registers.put(i,int2DoubleInSameBits(-10));
                }
                System.out.println(registers.get(i));
            }else {
                registers.put(i,0.0);
            }
        }
    }

    private double int2DoubleInSameBits(int value){
        System.out.println(Long.toHexString(value&0xFFFFFFFFL));
        return Double.longBitsToDouble(value & 0xFFFFFFFFL);
    }

    @Override
    public void run() {
        running = true;
        if (port.openPort()){
            // Set the communication parameters (baud rate, data bits, stop bits, parity)
            port.setBaudRate(BAUD_RATE);
            port.setNumDataBits(NUM_DATA_BITS);
            port.setNumStopBits(NUM_STOP_BITS);
            port.setParity(PARITY);
            port.addDataListener(this);

            while (running){

            }
            port.closePort();
        }
        System.out.println("Finish PLCSimulator");
    }

    public boolean isRunning() {
        return running;
    }

    public void stop(){
        running = false;
    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
            return;

        byte[] newData = new byte[port.bytesAvailable()];
        port.readBytes(newData, newData.length);//read buffer
        System.out.println("Receive new data");
        try {
            SerialCommand cmd = new SerialCommand(index,newData);
            if (cmd.getType() == CommandType.WRITE){
                registers.put(cmd.getRegisterAddressInDec(),cmd.getDataInDouble());
                byte[] response = cmd.response(0);
                port.writeBytes(response,response.length);
            }else {
                byte[] response = cmd.response(registers.get(cmd.getRegisterAddressInDec()));
                port.writeBytes(response,response.length);
            }
        }catch (Exception e){
            System.err.println(Utils.stackStraceString(e));
            port.writeBytes(new byte[]{0x15},1);
        }
    }
}
