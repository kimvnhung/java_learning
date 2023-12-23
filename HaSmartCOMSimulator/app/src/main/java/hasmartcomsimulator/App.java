/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package hasmartcomsimulator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hasmart.*;
import hasmart.primaryInterface.PLCIndex;

public class App {

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        PLCSimulator plc1 = new PLCSimulator(PLCIndex.PLC1,"COM3");
        PLCSimulator plc2 = new PLCSimulator(PLCIndex.PLC2,"COM4");
        executorService.submit(plc1);
        executorService.submit(plc2);
        System.out.println("startListen");
        //wait for terminated
        while (true){

        }
    }
}
