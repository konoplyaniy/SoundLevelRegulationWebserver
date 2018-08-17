package com.volume.changer;

import spark.Spark;

import java.io.BufferedReader;
import java.io.IOException;

import java.io.InputStreamReader;

public class VolumeChanger {
    private static final int HTTP_BAD_REQUEST = 400;
    private static VolumeChanger volumeChanger;

    private void setVolume() {
        Spark.get("/api-change-volume", (((request, response) -> {
            System.out.println("trying to change volume");
            response.status(200);
            response.type("text/html");

            int volumeNewValue;
            String requestedValue = "";

            if (null == request.queryParams("value") && !request.queryParams("value").isEmpty()) {
                return -1;
            }
            try {
                requestedValue = request.queryParams("value");
                volumeNewValue = Integer.parseInt(requestedValue);
            } catch (NumberFormatException e) {
                System.out.println("Can't parse [" + requestedValue + "] to String");
                return -1;
            }
            new VolumeChanger().runCommandAndGetResult("C:\\SR\\win10-x64\\AC.exe set " +
                    (volumeNewValue != 0 ? requestedValue : 5));
            return volumeNewValue;
        })));
    }

    private void increaseVolume() {
        Spark.get("/api-increase-volume", (((request, response) -> {
            System.out.println("increase volume");
            response.status(200);
            response.type("text/html");
            return incDecVolume(true);
        })));
    }

    private void decreaseVolume() {
        Spark.get("/api-decrease-volume", (((request, response) -> {
            System.out.println("decrease volume");
            response.status(200);
            response.type("text/html");
            incDecVolume(false);
            return incDecVolume(false);
        })));
    }

    private void getCurrentVolume() {
        Spark.get("/api-get-volume", (((request, response) -> {
            System.out.println("get volume");
            response.status(200);
            response.type("text/html");
            return new VolumeChanger().runCommandAndGetResult("C:\\SR\\win10-x64\\AC.exe get");
        })));
    }

    private void home() {
        Spark.get("/", (((request, response) -> {
            response.status(200);
            response.type("text/html");
            String currentVolume = new VolumeChanger().runCommandAndGetResult("C:\\SR\\win10-x64\\AC.exe get");
            return "current volume: " + currentVolume;
        })));
    }

    private int incDecVolume(boolean increase) {
        int currentVolume = Integer.parseInt(
                volumeChanger.runCommandAndGetResult("C:\\SR\\win10-x64\\AC.exe get")
                        .replace("Current volume: ", ""));
        System.out.println("current volume: " + currentVolume);
        if (increase) {
            System.out.println("try to increase volume");
            volumeChanger.runCommandAndGetResult("C:\\SR\\win10-x64\\AC.exe set " + (currentVolume + 10));
            return currentVolume + 10;
        } else {
            if (currentVolume > 10) {
                System.out.println("try to decrease volume");
                volumeChanger.runCommandAndGetResult("C:\\SR\\win10-x64\\AC.exe set " + (currentVolume - 10));
                return currentVolume - 10;
            } else {
                volumeChanger.runCommandAndGetResult("C:\\SR\\win10-x64\\AC.exe set 5");
                return 5;
            }
        }
    }

    private String runCommandAndGetResult(String commandToExec) {
        Runtime rt = Runtime.getRuntime();
        /*String[] commands = {"cd C:\\AC\\","dotnet run get"};*/

        Process process = null;
        try {
            /*process = rt.exec("dotnet run --project C:\\AC\\ get");*/
            process = rt.exec(commandToExec);
        } catch (IOException e) {
            System.out.println("IOException while run command");
            e.printStackTrace();
        }

        return getVolumeFromProcess(process);
    }

    private String getVolumeFromProcess(Process process) {
        if (null == process) {
            return "ERROR: Can't return volume from process - process object is null!";
        }
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(process.getInputStream()));

        // read the output from the command
        String s = "";
        try {
            while ((s = stdInput.readLine()) != null) {
                if (s.startsWith("Current volume: ")) {
                    return s.replace("Current volume: ", "");
                }
            }
        } catch (IOException e) {
            System.out.println("IO Exception while read command output!");
            return e.getLocalizedMessage();
        }
        return "ERROR: Can't return volume! something is going wrong =(";
    }

    private void powerOff() {
        Spark.get("/api-turn-off", (((request, response) -> {
            System.out.println("trying to switch off");
            response.status(200);
            response.type("text/html");

            int timeToOff;
            String requestedValue = "";
            String status;

            if (null == request.queryParams("time") && !request.queryParams("time").isEmpty()) {
                return -1;
            }
            try {
                requestedValue = request.queryParams("time");
                timeToOff = Integer.parseInt(requestedValue);
                System.out.println("requested time to off: " + timeToOff);
                status = new VolumeChanger().powerOff(timeToOff);
            } catch (NumberFormatException e) {
                System.out.println("Can't parse [" + requestedValue + "] to String");
                return "on";
            }

            return status;
        })));
    }

    /**
     * @param timeToOff time to off PC in seconds
     * @return on, if occur some error while run command,
     * -off, if all is ok
     */
    private String powerOff(int timeToOff) {
        Runtime rt = Runtime.getRuntime();
        try {
            rt.exec("shutdown /s /t " + timeToOff + " /f");
        } catch (IOException e) {
            System.out.println("IOException while run command");
            e.printStackTrace();
            return "on";
        }
        return "off";
    }

    public static void main(String[] args) {
        volumeChanger = new VolumeChanger();
        volumeChanger.home();
        System.out.println("Init functions:");
        System.out.println("Set volume");
        volumeChanger.setVolume();
        System.out.println("Get current volume");
        volumeChanger.getCurrentVolume();
        System.out.println("Increase volume");
        volumeChanger.increaseVolume();
        System.out.println("Decrease volume");
        volumeChanger.decreaseVolume();
        System.out.println("Turn off function");
        volumeChanger.powerOff();
    }
}
