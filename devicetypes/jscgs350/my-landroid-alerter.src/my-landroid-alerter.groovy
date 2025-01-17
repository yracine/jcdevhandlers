/**
 *  LANnouncer Alerter (Formerly LANdroid - but Google didn't like that much.)
 *
 *  Requires the LANnouncer android app; https://play.google.com/store/apps/details?id=com.keybounce.lannouncer
 *  See http://www.keybounce.com/LANdroidHowTo/LANdroid.html for full downloads and instructions.
 *  SmartThings thread: https://community.smartthings.com/t/android-as-a-speech-alarm-device-released/30282/12
 *
 *  Version 1.14, 30 Dec 2015
 *
 *  Copyright 2015 Tony McNamara
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
    definition (name: "My LANdroid Alerter", namespace: "jscgs350", author: "Tony McNamara") {
        capability "Alarm"
        capability "Speech Synthesis"
        capability "Notification"
        capability "Tone"
        capability "Image Capture"
        attribute  "LANdroidSMS","string"
        /* Per http://docs.smartthings.com/en/latest/device-type-developers-guide/overview.html#actuator-and-sensor */
        capability "Sensor"
        capability "Actuator"
    }
    preferences {
        input("DeviceLocalLan", "string", title:"Android IP Address", description:"Please enter your tablet's I.P. address", defaultValue:"" , required: false, displayDuringSetup: true)
        input("DevicePort", "string", title:"Android Port", description:"Port the Android device listens on", defaultValue:"1035", required: false, displayDuringSetup: true)
        input("ReplyOnEmpty", "bool", title:"Say Nothing", description:"When no speech is found, announce LANdroid?  (Needed for the speech and notify tiles to work)", defaultValue: true, displayDuringSetup: true)
    }

    simulator {
        
    }

    tiles {
        standardTile("alarm", "device.alarm", width: 2, height: 2) {
            state "off", label:'off', action:'alarm.both', icon:"st.alarm.alarm.alarm", backgroundColor:"#ffffff"
            state "strobe", label:'strobe!', action:'alarm.off', icon:"st.Lighting.light11", backgroundColor:"#e86d13"
            state "siren", label:'siren!', action:'alarm.off', icon:"st.alarm.alarm.alarm", backgroundColor:"#e86d13"
            state "both", label:'alarm!', action:'alarm.off', icon:"st.alarm.alarm.alarm", backgroundColor:"#e86d13"
        }
        standardTile("strobe", "device.alarm", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"alarm.strobe", icon:"st.secondary.strobe"
        }
        
        standardTile("siren", "device.alarm", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"alarm.siren", icon:"st.secondary.siren"
        }       
        
        /* Apparently can't show image attributes on tiles. */
        standardTile("take", "device.image", width: 1, height: 1, canChangeIcon: false, canChangeBackground: false) 
        {
            state "take", label: "Take", action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
            state "taking", label:'Taking', action: "", icon: "st.camera.take-photo", backgroundColor: "#53a7c0"
            state "image", label: "Take", action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
        }

        
        standardTile("speak", "device.speech", inactiveLabel: false, decoration: "flat") 
        {
            state "default", label:'Speak', action:"Speech Synthesis.speak", icon:"st.Electronics.electronics13"
        }
        standardTile("toast", "device.notification", inactiveLabel: false, decoration: "flat") {
            state "default", label:'Notify', action:"notification.deviceNotification", icon:"st.Kids.kids1"
        }
        standardTile("beep", "device.tone", inactiveLabel: false, decoration: "flat") {
            state "default", label:'Tone', action:"tone.beep", icon:"st.Entertainment.entertainment2"
        }
        carouselTile("cameraDetails", "device.image", width: 3, height: 2) { }

        main (["alarm", "take"]);
        details(["alarm","strobe","siren","speak", "take","toast","beep", "cameraDetails"]);
    }
}

/** Generally matches TTSServer/app/build.gradle */
String getVersion() {return "11";}

// handle commands
def off() {
    log.debug "Executing 'off'"
    // TODO: handle 'off' command
}

def strobe() {
    log.debug "Executing 'strobe'"
    // TODO: handle 'strobe' command
    def command="&FLASH=STROBE&"+getDoneString()
    sendCommands(command)
}

def siren() {
    log.debug "Executing 'siren'"
    // TODO: handle 'siren' command
    def command="&ALARM=SIREN&"+getDoneString()
    sendCommands(command)
}

def beep() {
    log.debug "Executing 'beep'"
    // TODO: handle 'siren' command
    def command="&ALARM=CHIME&"+getDoneString()
    sendCommands(command)
}

def both() {
    log.debug "Executing 'both'"
    // TODO: handle 'both' command
    def command="&ALARM=ON&FLASH=ON&"+getDoneString()
    sendCommands(command)
}


def speak(toSay) {
    log.debug "Executing 'speak'"
    if (!toSay?.trim()) {
        if (ReplyOnEmpty) {
            toSay = "LANnouncer Version ${version}"
        }
    }

    if (toSay?.trim()) {
        def command="&SPEAK="+toSay+"&"+getDoneString()
        sendCommands(command)
    }
}

def deviceNotification(toToast) {
    log.debug "Executing notification with "+toToast
    if (!toToast?.trim()) {
        if (ReplyOnEmpty) {
            toToast = "LANnouncer Version ${version}";
        }
    }
    if (toToast?.trim()) {
        def command="&TOAST="+toToast+"&"+getDoneString()
        sendCommands(command)
    }
}    

def take() {
    // This won't result in received file. Can't handle large or binaries in hub.
    log.debug "Executing 'take'"
    def command="&PHOTO=BACK&STSHRINK=TRUE&"+getDoneString()
    sendIPCommand(command, true)
}

/* Send to IP and to SMS as appropriate */
private sendCommands(command) {
    log.info "Command request: "+command
    sendSMSCommand(command)
    sendIPCommand(command)
}

private sendIPCommand(commandString, sendToS3 = false) {
    log.info "Sending command "+ commandString+" to "+DeviceLocalLan+":"+DevicePort
    if (DeviceLocalLan?.trim()) {
        def hosthex = convertIPtoHex(DeviceLocalLan)
        def porthex = convertPortToHex(DevicePort)
        device.deviceNetworkId = "$hosthex:$porthex"

        def headers = [:] 
        headers.put("HOST", "$DeviceLocalLan:$DevicePort")

        def method = "GET"

        def hubAction = new physicalgraph.device.HubAction(
            method: method,
            path: "/"+commandString,
            headers: headers
            );
        if (sendToS3 == true)
        {
            hubAction.options = [outputMsgToS3:true];
        }
        log.debug hubAction
        hubAction;
    }
}

private sendSMSCommand(commandString) {
    def preface = "+@TTSSMS@+"
    def smsValue = preface+"&"+commandString
    state.lastsmscommand = smsValue
    sendEvent(name: "LANdroidSMS", value: smsValue, isStateChange: true)
    /*
    if (SMSPhone?.trim()) {
        sendSmsMessage(SMSPhone, preface+"&"+commandString)
    }
    */
}

private String getDoneString() {
    return "@DONE@"
}

def parse(String description) {
    log.debug "Parsing '${description}'"
    def map = parseLanMessage(description);
    log.debug "As LAN: " + map;
    if ((map.headers) && (map.headers.'Content-Type' != null) && (map.headers.'Content-Type'.contains("image/jpeg")) )
    {   //  Store the file
      if(map.body) 
      {
            storeImage(getPictureName(), map.body);
      }
    }
/* 'index:0F, mac:0073E023A13A, ip:C0A80114, port:040B, requestId:f9036fb2-9637-40b8-b2c5-71ba5a09fd3e, bucket:smartthings-device-conn-temp, key:fc8e3dfd-5035-40a2-8adc-a312926f9034' */

    else if (map.bucket && map.key)
    { //    S3 pointer; retrieve image from it to store.
        try {
            def s3ObjectContent; // Needed for scope of try-catch
            def imageBytes = getS3Object(map.bucket, map.key + ".jpg")

            if(imageBytes)
            {
                log.info ("Got image bytes; saving them.")
                s3ObjectContent = imageBytes.getObjectContent()
                def bytes = new ByteArrayInputStream(s3ObjectContent.bytes)
                storeImage(getPictureName(), bytes)
            }
        }
        catch(Exception e) 
        {
            log.error e
        }
        finally {
            //explicitly close the stream
            if (s3ObjectContent) { s3ObjectContent.close() }
        }        
    }
}


// Image Capture handling
/* Note that images are stored in https://graph.api.smartthings.com/api/s3/smartthings-smartsense-camera/[IMAGE-ID4], 
 * where [IMAGE-ID] is listed in the IDE under My Devices > [Camera] > Current States: Image. That page is updated as pictures are taken.
 */


private getPictureName() {
    def pictureUuid = java.util.UUID.randomUUID().toString().replaceAll('-', '')
    log.debug pictureUuid
    def picName = device.deviceNetworkId.replaceAll(':', '') + "_$pictureUuid" + ".jpg"
    return picName
}


private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04X', port.toInteger() )
    log.debug hexport
    return hexport
}
