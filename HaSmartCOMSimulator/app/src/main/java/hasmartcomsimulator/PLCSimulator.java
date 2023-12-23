package hasmartcomsimulator;

import static hasmart.constants.Constants.BAUD_RATE;
import static hasmart.constants.Constants.NUM_DATA_BITS;
import static hasmart.constants.Constants.NUM_STOP_BITS;
import static hasmart.constants.Constants.PARITY;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
                    registers.put(i,int2DoubleInSameBits(1662640997));
                }else {
                    registers.put(i,int2DoubleInSameBits(-1852250592));
                }
                System.out.println(registers.get(i));
            } else if (i == 602) {
                if (index == PLCIndex.PLC1){
                    registers.put(i,int2DoubleInSameBits(1625926211));
                }else {
                    registers.put(i,int2DoubleInSameBits(585081218));
                }
                System.out.println(registers.get(i));
            } else if (i == 604) {
                if (index == PLCIndex.PLC1){
                    registers.put(i,int2DoubleInSameBits(1203708648));
                }else {
                    registers.put(i,int2DoubleInSameBits(-617059527));
                }
                System.out.println(registers.get(i));
            } else {
                registers.put(i,0.0);
            }
        }
    }

    private double int2DoubleInSameBits(int value){
//        System.out.println(Long.toHexString(value&0xFFFFFFFFL));
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
            //generate alarmcode array
            int[] alarmCode = new int[25];
            for (int i = 0; i < alarmCode.length; i++) {
                if (i >= 1 && i <= 18){
                    if (i != 18){
                        alarmCode[i] = 100+i;
                    }else {
                        alarmCode[i] = 201;
                    }
                }else {
                    alarmCode[i] = i;
                }
            }


            while (running){
                randomAlarmCode(alarmCode);
                try {
                    Random rd = new Random();
                    int randomInt = rd.nextInt(8000)+2000;
                    Thread.sleep(randomInt);
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }
            }
            port.closePort();
        }
        System.out.println("Finish PLCSimulator");
    }

    private void randomAlarmCode(int[] alarmCode) {
        Random random = new Random();
        int randomInt = random.nextInt(25)-1;
        System.out.printf("write alarmcde %d\n",alarmCode[randomInt]);
        registers.put(100,int2DoubleInSameBits(alarmCode[randomInt]));
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
        System.out.println("Receive new data at "+index.name());
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
