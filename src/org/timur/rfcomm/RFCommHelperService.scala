/*
 * This file is part of RFComm and AnyMime, a program to help you swap
 * files wirelessly between mobile devices.
 *
 * Copyright (C) 2012 Timur Mehrvarz, timur.mehrvarz(a)gmail(.)com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.timur.rfcomm

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.FileWriter
import java.util.UUID
import java.util.LinkedList
import java.util.ArrayList
import java.util.Calendar
import java.net.Socket
import java.net.ServerSocket
import java.net.InetSocketAddress
import java.net.InetAddress

import android.app.Activity
import android.app.ActivityManager
import android.app.PendingIntent
import android.util.Log
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentFilter.MalformedMimeTypeException
import android.content.BroadcastReceiver
import android.content.SharedPreferences
import android.os.IBinder
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.os.Environment
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.provider.Settings
import android.widget.Toast

import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.WifiManager
import android.net.NetworkInfo

import android.nfc.NfcAdapter
import android.nfc.NdefMessage
import android.nfc.tech.NfcF
import java.util.Locale

object RFCommHelperService {
  @scala.reflect.BeanProperty val STATE_NONE = 0        // not connected and not listening for incoming connections
  @scala.reflect.BeanProperty val STATE_LISTEN = 1      // not connected but listening for incoming connections
  @scala.reflect.BeanProperty val STATE_CONNECTING = 2  // connecting
  @scala.reflect.BeanProperty val STATE_CONNECTED = 3   // connected to at least one remote device


  // Message types sent from RFCommHelperService to the activity handler
  @scala.reflect.BeanProperty val MESSAGE_STATE_CHANGE = 1
  @scala.reflect.BeanProperty val DEVICE_DISCONNECT = 7 // todo: how is this different from MESSAGE_STATE_CHANGE ?

  @scala.reflect.BeanProperty val CONNECTION_START = 9
  //not being used: @scala.reflect.BeanProperty val CONNECTING = 16       // todo: how is this different from CONNECTION_START

  @scala.reflect.BeanProperty val CONNECTION_FAILED = 8 // todo: how is this different from MESSAGE_STATE_CHANGE ?

  @scala.reflect.BeanProperty val MESSAGE_DEVICE_NAME = 4 // todo: how is this different from MESSAGE_STATE_CHANGE ?

  @scala.reflect.BeanProperty val UI_UPDATE = 14        // non-alert message
  @scala.reflect.BeanProperty val ALERT_MESSAGE = 15    // temporary (=toast)
  @scala.reflect.BeanProperty val PERSIST_MESSAGE = 17  // stays until replaced (or confirmed?)

  // Key names received from Service to the activity handler
  val DEVICE_NAME = "device_name"
  val DEVICE_ADDR = "device_addr"
} 

class RFCommHelperService extends android.app.Service {
  // public objects
  @volatile var activityResumed = false   // set by RFCommHelper
  @volatile var state = RFCommHelperService.STATE_NONE  // retrieved by activity
  @volatile var p2pWifiDiscoveredCallbackFkt:(WifiP2pDevice) => Unit = null
  var activity:Activity = null            // set by activity on new ServiceConnection()
  var activityMsgHandler:Handler = null   // set by activity on new ServiceConnection()
  var appService:RFServiceTrait = null
  var connectedRadio:Int = 0              // todo: not happy with this implementation, better use RFCommHelper.RADIO_xxxxx flags
  val wifiP2pDeviceArrayList = new ArrayList[WifiP2pDevice]()
  var discoveringPeersInProgress = false  // so we do not call discoverPeers() again while it is active still
  var isWifiP2pEnabled = false            // if false in onResume, we will offer ACTION_WIRELESS_SETTINGS 
  var p2pConnected = false                // set and cleared in WiFiDirectBroadcastReceiver
  var wifiP2pManager:WifiP2pManager = null
  var p2pChannel:Channel = null
  var localP2pWifiAddr:String = null      // set and used in WiFiDirectBroadcastReceiver
  var mNfcAdapter:NfcAdapter = null
  var nfcPendingIntent:PendingIntent = null
  var nfcFilters:Array[IntentFilter] = null
  var nfcTechLists:Array[Array[String]] = null
  var ipPort = 8954
  var appName:String = null  // should be the package name, used for nfc intents
  var activityRuntimeClass:java.lang.Class[Activity] = null  // needed for nfcPendingIntent only
  var nfcForegroundPushMessage:NdefMessage = null
  var desiredBluetooth = false
  var pairedBtOnly = false // support insecure bt if set false, false value only evaluated for sdk>=14 (4.0+)
  var desiredWifiDirect = false
  var desiredNfc = false
  var prefsSharedP2pBt:SharedPreferences = null
  var prefsSharedP2pBtEditor:SharedPreferences.Editor = null
  var prefsSharedP2pWifi:SharedPreferences = null
  var prefsSharedP2pWifiEditor:SharedPreferences.Editor = null
  var acceptThreadSecureName:String = null
  var acceptThreadSecureUuid:String = null
  var mSecureAcceptThread:AcceptThread = null
  var acceptThreadInsecureName:String = null
  var acceptThreadInsecureUuid:String = null
  var mInsecureAcceptThread:AcceptThread = null

  // private objects
  private val TAG = "RFCommHelperService"
  private val D = true
  @volatile private var mConnectThread:ConnectThreadBt = null

  if(D) Log.i(TAG, "constructor try to access BluetoothAdapter.getDefaultAdapter...")
  private val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter
  private var myBtName = if(mBluetoothAdapter!=null) mBluetoothAdapter.getName else null
  private var myBtAddr = if(mBluetoothAdapter!=null) mBluetoothAdapter.getAddress else null  

  if(D) Log.i(TAG, "constructor myBtName="+myBtName+" myBtAddr="+myBtAddr+" mBluetoothAdapter="+mBluetoothAdapter)
  private var blobTaskId = 0

  class LocalBinder extends android.os.Binder {
    def getService = RFCommHelperService.this
  }
  private val localBinder = new LocalBinder
  override def onBind(intent:Intent) :IBinder = localBinder 

  override def onCreate() {
    //if(D) Log.i(TAG, "onCreate")
  }

  // called by Activity onResume() 
  // but only if state == STATE_NONE
  // this is why we quickly switch state to STATE_LISTEN
  def startBtAcceptThreads() = synchronized {
    if(D) Log.i(TAG, "startBtAcceptThreads android.os.Build.VERSION.SDK_INT="+android.os.Build.VERSION.SDK_INT+" pairedBtOnly="+pairedBtOnly+" activityResumed="+activityResumed)
    // in case bt was turned on _after_ app start
    if(myBtAddr==null) {
      myBtAddr = mBluetoothAdapter.getAddress
      if(myBtAddr==null)
        myBtAddr = "unknown"  // todo
    }
    if(myBtName==null) {
      myBtName = mBluetoothAdapter.getName
      if(myBtName==null)
        myBtName = "unknown"  // todo
    }
    if(D) Log.i(TAG, "startBtAcceptThreads myBtName="+myBtName+" myBtAddr="+myBtAddr+" mBluetoothAdapter="+mBluetoothAdapter+" pairedBtOnly="+pairedBtOnly)

    // start thread to listen on BluetoothServerSocket
    if(mSecureAcceptThread == null) {
      if(D) Log.i(TAG, "startBtAcceptThreads new AcceptThread for secure")
      mSecureAcceptThread = new AcceptThread(true)
      if(mSecureAcceptThread != null) 
        mSecureAcceptThread.start
    }

    if(android.os.Build.VERSION.SDK_INT>=10 && !pairedBtOnly) {
      if(mInsecureAcceptThread == null) {
        if(D) Log.i(TAG, "startBtAcceptThreads new AcceptThread for insecure (running on 4.0+)")
        mInsecureAcceptThread = new AcceptThread(false)
        if(mInsecureAcceptThread != null)
          mInsecureAcceptThread.start
      }
    }

    setState(RFCommHelperService.STATE_LISTEN)   // this will send MESSAGE_STATE_CHANGE
    if(D) Log.i(TAG, "startBtAcceptThreads done")
  }

  // called by onDestroy() + by activity
  def stopActiveConnection() {
    if(appService==null)
      if(D) Log.i(TAG, "stopActiveConnection mConnectThread="+mConnectThread+" appService==null")
    else
      if(D) Log.i(TAG, "stopActiveConnection mConnectThread="+mConnectThread+" appService.connectedThread="+appService.connectedThread)

    // we start a separate thread, in order to prevent android.os.NetworkOnMainThreadException
    if(mConnectThread!=null || (appService!=null && appService.connectedThread!=null)) {
      new Thread() {
        override def run() {
          if(appService!=null && appService.connectedThread!=null) {
            // disconnect in case we were the connect responder
            if(D) Log.i(TAG, "stopActiveConnection appService.connectedThread="+appService.connectedThread)
            appService.connectedThread.cancel
            appService.connectedThread = null
          }
          if(mConnectThread != null) {
            // disconnect in case we were the connect initiator
            mConnectThread.cancel
            mConnectThread = null
          }
          setState(RFCommHelperService.STATE_LISTEN)   // will send MESSAGE_STATE_CHANGE to activity
          if(D) Log.i(TAG, "stopActiveConnection done")
        }
      }.start

    } else {
      setState(RFCommHelperService.STATE_LISTEN)   // will send MESSAGE_STATE_CHANGE to activity
      if(D) Log.i(TAG, "stopActiveConnection setState STATE_LISTEN only")
    }
  }

  def stopAcceptThreads() = synchronized {
    if(mSecureAcceptThread != null) {
      mSecureAcceptThread.cancel
      mSecureAcceptThread = null
    }
    if(!pairedBtOnly && mInsecureAcceptThread!=null) {
      mInsecureAcceptThread.cancel
      mInsecureAcceptThread = null
    }
  }

  // called by onNewIntent(): as a result of NfcAdapter.ACTION_NDEF_DISCOVERED
  // called by the activity: options menu "connect" -> onActivityResult() -> connectDevice()
  def connectBt(newRemoteDevice:BluetoothDevice, reportConnectState:Boolean=true, onstartEnableBackupConnection:Boolean=false) :Unit = synchronized {
    if(onstartEnableBackupConnection==false && newRemoteDevice==null) {
      Log.e(TAG, "connect() remoteDevice==null, abort")
      return
    }

    //connectedRadio = 1 // bt    // todo: premature? will be set again in connectedBt()
    
    state = RFCommHelperService.STATE_CONNECTING

    if(newRemoteDevice!=null) {
      if(D) Log.i(TAG, "connectBt remoteAddr="+newRemoteDevice.getAddress+" name=["+newRemoteDevice.getName+"] pairedBtOnly="+pairedBtOnly)

      // store target deviceAddr and deviceName to "org.timur.p2pDevices" preferences
      if(newRemoteDevice.getName!=null && newRemoteDevice.getName.length>0) {
        if(prefsSharedP2pBtEditor!=null) {
          val existingName = prefsSharedP2pBt.getString(newRemoteDevice.getAddress,null)
          if(existingName==null || existingName.length==0 || existingName=="nfc-target") {
            prefsSharedP2pBtEditor.putString(newRemoteDevice.getAddress,newRemoteDevice.getName)
            prefsSharedP2pBtEditor.commit
          }
        }
      }

      if(reportConnectState && activityMsgHandler!=null) {
        val msg = activityMsgHandler.obtainMessage(RFCommHelperService.CONNECTION_START)
        val bundle = new Bundle
        bundle.putString(RFCommHelperService.DEVICE_ADDR, newRemoteDevice.getAddress)
        bundle.putString(RFCommHelperService.DEVICE_NAME, newRemoteDevice.getName)
        msg.setData(bundle)
        activityMsgHandler.sendMessage(msg)
      }
    }
    
    // Start the thread to connect with the given device
    mConnectThread = new ConnectThreadBt(newRemoteDevice, reportConnectState, onstartEnableBackupConnection)
    mConnectThread.start
  }

  def connectWifi(p2pWifiAddr:String, p2pWifiName:String, 
                  onlyIfLocalAddrBiggerThatRemote:Boolean, reportConnectState:Boolean=true,
                  onstartEnableBackupConnection:Boolean=false) :Unit = synchronized {
    // todo: onstartEnableBackupConnection not yet being evaluated
    
    if(wifiP2pManager==null) {
      if(D) Log.i(TAG, "connectWifi wifiP2pManager==null abort")
      return
    }

    // wrong attempt to fix p2pWifi issues
    //p2pChannel = wifiP2pManager.initialize(activity, activity.getMainLooper, null)

    connectedRadio = 2 // wifi
    state = RFCommHelperService.STATE_CONNECTING

    // store target deviceAddr and deviceName to "org.timur.p2pDevices" preferences
    if(p2pWifiAddr!=null && p2pWifiName!=null && p2pWifiName.length>0) {
      if(prefsSharedP2pWifiEditor!=null) {
        // but don't overwrite if one entry with p2pWifiAddr (and a name?) exists     
        val existingName = prefsSharedP2pWifi.getString(p2pWifiAddr,null)
        if(existingName==null || existingName.length==0 || existingName=="nfc-target") {
          prefsSharedP2pWifiEditor.putString(p2pWifiAddr,p2pWifiName)
          prefsSharedP2pWifiEditor.commit
        }
      }
    }

    if(onlyIfLocalAddrBiggerThatRemote && localP2pWifiAddr<p2pWifiAddr) {
      // this is to prevent nfc-initiated concurrent connect requests 
      if(D) Log.i(TAG, "connectWifi local="+localP2pWifiAddr+" < remote="+p2pWifiAddr+" - stay passive - let other device connect() ############")

    } else {
      if(onlyIfLocalAddrBiggerThatRemote)
        if(D) Log.i(TAG, "connectWifi active connect() local="+localP2pWifiAddr+" > remote="+p2pWifiAddr+" ############")
      else
        if(D) Log.i(TAG, "connectWifi active connect() local="+localP2pWifiAddr+" to remote="+p2pWifiAddr+" ############")
      val wifiP2pConfig = new WifiP2pConfig()
      wifiP2pConfig.groupOwnerIntent = -1
      wifiP2pConfig.wps.setup = WpsInfo.PBC
      wifiP2pConfig.deviceAddress = p2pWifiAddr
      wifiP2pManager.connect(p2pChannel, wifiP2pConfig, new ActionListener() {
        override def onSuccess() {
          if(D) Log.i(TAG, "connectWifi wifiP2pManager.connect() success")
          // we expect WIFI_P2P_CONNECTION_CHANGED_ACTION in WiFiDirectBroadcastReceiver to notify us
          // however sometimes this does NOT happen
          // todo tmtmtm: this is why we still need to implement "connect-request auto-abort crierias" !!!
          // criteria 1: we receive PEERS_CHANGED_ACTION with deviceAddress=[our target mac addr] and status=failed
          // criteria 2: a simple timeout as a safty belt
          
          // let's render the connect-progress animation (like we do in connectBt)
          connectedRadio = 2 // wifi    // todo: duplicate - likely not necessary
          if(activityMsgHandler!=null) {
            val msg = activityMsgHandler.obtainMessage(RFCommHelperService.CONNECTION_START)
            val bundle = new Bundle
            bundle.putString(RFCommHelperService.DEVICE_ADDR, wifiP2pConfig.deviceAddress)
            bundle.putString(RFCommHelperService.DEVICE_NAME, p2pWifiName)
            msg.setData(bundle)
            activityMsgHandler.sendMessage(msg)
          }
        }

        override def onFailure(reason:Int) {
          val errMsg = "wifiP2pManager.connect() failed reason="+reason
          Log.e(TAG, "connectWifi fail "+errMsg+" ##################")
          // reason ERROR=0, P2P_UNSUPPORTED=1, BUSY=2
          if(activityMsgHandler!=null)
            activityMsgHandler.obtainMessage(RFCommHelperService.ALERT_MESSAGE, -1, -1, errMsg).sendToTarget
        }
      })
      if(D) Log.i(TAG, "connectWifi wifiP2pManager.connect() done")
    }
  }

  def getWifiIpAddr() :String = {
    if(activity==null) {
      Log.e(TAG, "getWifiIpAddr() activity==null")
      return null
    }
    val wifiManager = activity.getSystemService(Context.WIFI_SERVICE).asInstanceOf[WifiManager]
    if(wifiManager==null) {
      Log.e(TAG, "getWifiIpAddr() wifiManager==null")
      return null
    }
    try {
      var wifiInfo:android.net.wifi.WifiInfo = null;
      wifiInfo = wifiManager.getConnectionInfo
      if(wifiInfo==null) {
        Log.e(TAG, "getWifiIpAddr() wifiInfo==null")
        return null
      }
      val ipAddrInt = wifiInfo.getIpAddress
      if(ipAddrInt==0) {
        if(D) Log.i(TAG, "getWifiIpAddr() ipAddrInt==0 - we got no std wifi addr")
        return null
      }
      val ipAddrString = "%d.%d.%d.%d".format((ipAddrInt & 0xff),
                                              (ipAddrInt >> 8 & 0xff),
                                              (ipAddrInt >> 16 & 0xff),
                                              (ipAddrInt >> 24 & 0xff))
      if(D) Log.i(TAG, "getWifiIpAddr() return ipAddrString="+ipAddrString)
      return ipAddrString

    } catch {
      case secureEx:java.lang.SecurityException =>
        Log.e(TAG, "getWifiIpAddr() java.lang.SecurityException: android.permission.ACCESS_WIFI_STATE not set")
        // android.permission.ACCESS_WIFI_STATE not set - ignore
    }
    return null
  }

  def connectIp(targetIpAddr:String, ipName:String, reportConnectState:Boolean=true) :Unit = synchronized {
    connectedRadio = 3 // access-point ip
    state = RFCommHelperService.STATE_CONNECTING

    val localAddr = getWifiIpAddr
    if(localAddr==null) {
      Log.e(TAG, "connectIp() localAddr==null abort")
      return
    }
    
    if(localAddr < targetIpAddr) {
      if(D) Log.i(TAG, "connectIp() as socketserver localAddr="+localAddr+" to targetIpAddr="+targetIpAddr)
      ipClientConnectorThread(true, null, null)  // socketserver
    }
    else {
      if(D) Log.i(TAG, "connectIp() as socket client localAddr="+localAddr+" to targetIpAddr="+targetIpAddr)
      ipClientConnectorThread(false, java.net.InetAddress.getByName(targetIpAddr), null)  // client socket
    }
  }

  class AcceptThread(pairedBt:Boolean=true) extends Thread {
    private var mmServerSocket:BluetoothServerSocket = null
    mmServerSocket = null

    try {
      if(pairedBt) {
        mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(acceptThreadSecureName, UUID.fromString(acceptThreadSecureUuid))
      } else {
        try {
          mmServerSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(acceptThreadInsecureName, UUID.fromString(acceptThreadInsecureUuid))
        } catch {
          case nsmerr: java.lang.NoSuchMethodError =>
            // this should really not happen, because we run the insecure method only if os>=4.0/level=14
            Log.e(TAG, "AcceptThread pairedBt="+pairedBt+" listenUsingInsecureRfcommWithServiceRecord failed", nsmerr)
        }
      }
    } catch {
      case e: IOException =>
        Log.e(TAG, "AcceptThread pairedBt="+pairedBt+" listen() failed", e)
    }

    override def run() {
      if(mmServerSocket==null) {
        Log.e(TAG, "AcceptThread pairedBt="+pairedBt+" run mmServerSocket==null")
        return
      }

      if(D) Log.i(TAG, "AcceptThread pairedBt="+pairedBt+" run mmServerSocket="+mmServerSocket)
      setName("AcceptThread"+pairedBtOnly)
      var btSocket:BluetoothSocket = null

      // Listen to the server socket if we're not connected
      while(mmServerSocket!=null) {
        if(D) Log.i(TAG, "AcceptThread pairedBt="+pairedBt+" run loop mmServerSocket="+mmServerSocket)
        try {
          synchronized {
            btSocket = null
            if(mmServerSocket!=null) {
              // This is a blocking call and will only return on a successful connection or an exception
              btSocket = mmServerSocket.accept
              if(D) Log.i(TAG, "AcceptThread pairedBt="+pairedBt+" run loop after accept, btSocket="+btSocket)
            }
          }
        } catch {
          case ioex: IOException =>
            // log exception only if not stopped
            if(state != RFCommHelperService.STATE_NONE)
              Log.e(TAG, "AcceptThread pairedBt="+pairedBt+" run state="+state+" ioex="+ioex)
        }

        if(D) Log.i(TAG, "AcceptThread pairedBt="+pairedBt+" btSocket="+btSocket+" activityResumed="+activityResumed)
        if(btSocket==null) {
          Log.e(TAG, "AcceptThread pairedBt="+pairedBt+" btSocket==null")

        } else {
          // btSocket!=null, store the deviceAddr and deviceName of the calling bt device
          val btDevice = btSocket.getRemoteDevice
          if(btDevice!=null) {
            if(btDevice.getName!=null && btDevice.getName.length>0) {
              if(prefsSharedP2pBtEditor!=null) {
                val existingName = prefsSharedP2pBt.getString(btDevice.getAddress,null)
                if(existingName==null || existingName.length==0 || existingName=="nfc-target") {
                  prefsSharedP2pBtEditor.putString(btDevice.getAddress,btDevice.getName)
                  prefsSharedP2pBtEditor.commit
                }
              }
            }
          }

          // a bt connection is now technically possible and can be accepted
          // note: this is where we can decide to deny based on activityResumed==false
          if(!activityResumed) {
            // our activity is currently paused
            if(D) Log.i(TAG, "AcceptThread pairedBt="+pairedBt+" Denying incoming connect request, activityResumed="+activityResumed+" activity="+activity)
            btSocket.close

            if(activity!=null) {
              AndrTools.runOnUiThread(activity) { () =>
                Toast.makeText(activity, "Run Anymime in foreground to accept BT connections.", Toast.LENGTH_LONG).show
              }
            }

          } else {
            // activity is not paused

            if(D) Log.i(TAG, "AcceptThread pairedBt="+pairedBt+" -> connectedBt()")
            RFCommHelperService.this synchronized {
              connectedBt(btSocket, btDevice)
            }
          }
        }
        
        // prevent tight loop
        try { Thread.sleep(100); } catch { case ex:Exception => }
      }

      // mmServerSocket was set to null
      if(D) Log.i(TAG, "AcceptThread pairedBt="+pairedBt+" end pairedBtOnly="+pairedBtOnly)
    }

    def cancel() { 
      if(D) Log.i(TAG, "AcceptThread pairedBt="+pairedBt+" cancel() mmServerSocket="+mmServerSocket)
      if(mmServerSocket!=null) {
        try {
          setState(RFCommHelperService.STATE_NONE)   // so that run() will NOT log an error; will send MESSAGE_STATE_CHANGE
          mmServerSocket.close
          mmServerSocket=null
        } catch {
          case ex: IOException =>
            Log.e(TAG, "AcceptThread pairedBt="+pairedBt+" cancel() mmServerSocket="+mmServerSocket+" ex=",ex)
        }
      }
    }
  }

  def setState(setState:Int) = synchronized {
    if(setState != state) {
      if(D) Log.i(TAG, "setState() "+state+" -> "+setState)
      state = setState
      // send modified state to the activity Handler
      if(activityMsgHandler!=null) {
        activityMsgHandler.obtainMessage(RFCommHelperService.MESSAGE_STATE_CHANGE, state, -1).sendToTarget
      } else {
        Log.e(TAG, "setState() failed to set "+setState+" because activityMsgHandler not set")
      }
    }
  }

  private class ConnectThreadBt(remoteDevice:BluetoothDevice, reportConnectState:Boolean=true, onstartEnableBackupConnection:Boolean=false) extends Thread {
    private var mmSocket:BluetoothSocket = null
    private var connectedUnpaired = false
    private var connectedPaired = false
    private var pairedBtOnlySession = pairedBtOnly

    override def run() {
      if(D) Log.i(TAG, "ConnectThreadBt run desiredBluetooth="+desiredBluetooth+" pairedBtOnly="+pairedBtOnlySession)
      setName("ConnectThreadBt"+pairedBtOnlySession)

      if(desiredBluetooth) {
        try {
          if(pairedBtOnlySession) {
            if(D) Log.i(TAG, "ConnectThreadBt run createRfcommSocketToServiceRecord(acceptThreadSecureUuid)")
            mmSocket = remoteDevice.createRfcommSocketToServiceRecord(UUID.fromString(acceptThreadSecureUuid))   // requires pairing
          } else {
            if(D) Log.i(TAG, "ConnectThreadBt run createInsecureRfcommSocketToServiceRecord(acceptThreadInsecureUuid)")
            mmSocket = remoteDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(acceptThreadInsecureUuid))   // does not require pairing
          }
        } catch {
          case e:IOException =>
            Log.e(TAG, "ConnectThreadBt Socket pairedBtOnly="+pairedBtOnlySession+" create() failed", e)
        }

        if(mmSocket!=null) {       
          if(!pairedBtOnlySession) {
            // check bondState
            if(D) Log.i(TAG, "ConnectThreadBt run check bondState")
            val serviceMgrClass = Class.forName("android.os.ServiceManager")
            val method = serviceMgrClass.getDeclaredMethod("getService", "".asInstanceOf[AnyRef].getClass)
            val iBinder = method.invoke(null,"bluetooth").asInstanceOf[IBinder]
            val classArray = Class.forName("android.bluetooth.IBluetooth").getDeclaredClasses
            //if(D) Log.i(TAG, "ConnectThreadBt run classArray="+classArray)
            val firstClass = classArray(0)    // android.bluetooth.IBluetooth.Stub
            //if(D) Log.i(TAG, "ConnectThreadBt run firstClass="+firstClass)
            val method2 = firstClass.getDeclaredMethod("asInterface", classOf[android.os.IBinder])
            method2.setAccessible(true)
            val ibt = method2.invoke(null,iBinder)            
            val bondState = ibt.asInstanceOf[{ def getBondState(macAddr:String) :Int }].getBondState(remoteDevice.getAddress)

/*
            // if not fully bonded => removeBond 
            // bondState may be "hidden bond" (10) which is < BluetoothDevice.BOND_BONDED (12)
            if(D) Log.i(TAG, "ConnectThreadBt run remoteDevice.bondState="+bondState+" #############################")
            if(bondState < BluetoothDevice.BOND_BONDED) {
*/
              val result = ibt.asInstanceOf[{ def removeBond(macAddr:String) :Boolean }].removeBond(remoteDevice.getAddress)
              if(D) Log.i(TAG, "ConnectThreadBt run remoteDevice.removeBond result="+result)
/*
            } else {
              // new: already fully paired: skip insecure connect attempt and only try secure connect
              if(D) Log.i(TAG, "ConnectThreadBt run skip unpaired connect for already paired device ##########################")
              mmSocket = remoteDevice.createRfcommSocketToServiceRecord(UUID.fromString(acceptThreadSecureUuid))
              pairedBtOnlySession = true
            }
*/
          }

          // "Always cancel discovery because it will slow down a connection"
          if(D) Log.i(TAG, "ConnectThreadBt run sleep before cancelDiscovery ...")
          try { Thread.sleep(300) } catch { case ex:Exception => }
          if(D) Log.i(TAG, "ConnectThreadBt run cancelDiscovery ...")
          mBluetoothAdapter.cancelDiscovery

          try {
            // This is a blocking call and will only return on a successful connection or an exception
            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            mmSocket.connect
            if(!pairedBtOnlySession)
              connectedUnpaired = true
            else
              connectedPaired = true
          } catch {
            case ex:IOException =>
              if(!pairedBtOnlySession) {
                Log.e(TAG, "ConnectThreadBt run ignore failed insecure connect",ex)
                // ignore this exception, try to connect again, but this time secure/paired

                try {
                  mmSocket.close
                } catch {
                  case ex:Exception =>
                    // ignore any exception on socket.close
                }

                // delay 2nd attempt
                try { Thread.sleep(750) } catch { case ex:Exception => }
                if(state==RFCommHelperService.STATE_CONNECTED) {  // we have been connected in parallel, don't report error
                  if(D) Log.i(TAG, "ConnectThreadBt abort, already connected")
                  return
                }

                try {
                  mmSocket = remoteDevice.createRfcommSocketToServiceRecord(UUID.fromString(acceptThreadSecureUuid))
                  if(D) Log.i(TAG, "ConnectThreadBt run 2nd attempt secure connect #########################")
                  mmSocket.connect
                  connectedPaired = true

                } catch {
                  case e: IOException =>
                    mmSocket = null
                    Log.e(TAG, "ConnectThreadBt run unable to connect() 2nd/secure attempt, onstartEnableBackupConnection="+onstartEnableBackupConnection,ex)
                    if(!onstartEnableBackupConnection) {
                      if(state!=RFCommHelperService.STATE_CONNECTED) {  // we have been connected in parallel, don't report error
                        if(reportConnectState) {
                          Log.i(TAG, "ConnectThreadBt report CONNECTION_FAILED")
                          val msg = activityMsgHandler.obtainMessage(RFCommHelperService.CONNECTION_FAILED)
                          val bundle = new Bundle
                          bundle.putString(RFCommHelperService.DEVICE_ADDR, remoteDevice.getAddress)
                          bundle.putString(RFCommHelperService.DEVICE_NAME, remoteDevice.getName)
                          msg.setData(bundle)
                          activityMsgHandler.sendMessage(msg)
                        }
                        Log.i(TAG, "ConnectThreadBt cancel connection")
                        cancel
                      }
                      return
                    }
                }
              } else {
                Log.e(TAG, "ConnectThreadBt run unable to connect() pairedBtOnly="+pairedBtOnlySession+" IOException",ex)
                mmSocket = null
                if(!onstartEnableBackupConnection) {
                  if(state!=RFCommHelperService.STATE_CONNECTED) {    // we have been connected in parallel, don't report error
                    if(reportConnectState) {
                      val msg = activityMsgHandler.obtainMessage(RFCommHelperService.CONNECTION_FAILED)
                      val bundle = new Bundle
                      bundle.putString(RFCommHelperService.DEVICE_ADDR, remoteDevice.getAddress)
                      bundle.putString(RFCommHelperService.DEVICE_NAME, remoteDevice.getName)
                      msg.setData(bundle)
                      activityMsgHandler.sendMessage(msg)
                    }
                    Log.i(TAG, "ConnectThreadBt cancel connection")
                    cancel
                  }
                  return
                }
              }
          }
        }
      }

      // Start the connected thread
      if(D) Log.i(TAG, "ConnectThreadBt -> connectedBt(mmSocket="+mmSocket+") paired="+connectedPaired)
      connectedBt(mmSocket, remoteDevice)
    }

    def cancel() {  // called by stopActiveConnection()
      if(D) Log.i(TAG, "ConnectThreadBt cancel() pairedBtOnly="+pairedBtOnlySession+" mmSocket="+mmSocket)
      if(mmSocket!=null) {
        try {
          mmSocket.close
        } catch {
          case e: IOException =>
            Log.e(TAG, "ConnectThreadBt cancel() socket.close() failed for pairedBtOnly="+pairedBtOnlySession, e)
        }
      }
      //setState(RFCommHelperService.STATE_LISTEN)   // will send MESSAGE_STATE_CHANGE to activity

      if(connectedUnpaired) {
        new Thread() {
          override def run() {
            try { Thread.sleep(1000) } catch { case ex:Exception => }

            // call check bondState
            if(D) Log.i(TAG, "ConnectThreadBt cancel check bondState")
            val serviceMgrClass = Class.forName("android.os.ServiceManager")
            val method = serviceMgrClass.getDeclaredMethod("getService", "".asInstanceOf[AnyRef].getClass)
            val iBinder = method.invoke(null,"bluetooth").asInstanceOf[IBinder]
            val classArray = Class.forName("android.bluetooth.IBluetooth").getDeclaredClasses
            //if(D) Log.i(TAG, "ConnectThreadBt cancel classArray="+classArray)
            val firstClass = classArray(0)    // android.bluetooth.IBluetooth.Stub
            //if(D) Log.i(TAG, "ConnectThreadBt cancel firstClass="+firstClass)
            val method2 = firstClass.getDeclaredMethod("asInterface", classOf[android.os.IBinder])
            method2.setAccessible(true)
            val ibt = method2.invoke(null,iBinder)
            val bondState = ibt.asInstanceOf[{ def getBondState(macAddr:String) :Int }].getBondState(remoteDevice.getAddress)
/*
            // if not fully bonded => removeBond 
            // bondState may be "hidden bond" (10) which is < BluetoothDevice.BOND_BONDED (12)
            if(D) Log.i(TAG, "ConnectThreadBt run remoteDevice.bondState="+bondState+" #############################")
            if(bondState < BluetoothDevice.BOND_BONDED) {
*/
              val result = ibt.asInstanceOf[{ def removeBond(macAddr:String) :Boolean }].removeBond(remoteDevice.getAddress)
              if(D) Log.i(TAG, "ConnectThreadBt cancel remoteDevice.removeBond result="+result)
/*
            }
*/
          }
        }.start
      }
    }
  }

  // called by: AcceptThread() -> btSocket = mmServerSocket.accept()
  // called by: ConnectThreadBt() / activity options menu (or NFC touch) -> connect() -> ConnectThreadBt()
  // called by: ConnectPopupActivity
  def connectedBt(btSocket:BluetoothSocket, remoteDevice:BluetoothDevice, 
                  argMap:scala.collection.immutable.HashMap[String,Any]=null) :Unit = synchronized {
    // in case of nfc triggered connect: for the device with the bigger btAddr, this is the 1st indication of the connect
    if(D) Log.i(TAG, "connectedBt, btSocket="+btSocket+" remoteDevice="+remoteDevice+" pairedBtOnly="+pairedBtOnly)
    var remoteBtAddrString:String = null
    var remoteBtNameString:String = null
    if(remoteDevice!=null) {
      if(btSocket!=null) {
        if(state==RFCommHelperService.STATE_CONNECTED) {  // todo: for as long as we only support one connection at a time
          if(D) Log.i(TAG, "connectedBt abort, already connected ##########")
          btSocket.close
          return
        }

        connectedRadio = 1 // bt
        //state = RFCommHelperService.STATE_CONNECTING
        setState(RFCommHelperService.STATE_CONNECTED)
      }

      remoteBtAddrString = remoteDevice.getAddress
      remoteBtNameString = remoteDevice.getName
      // convert spaces to underlines in btNameString (some android activities, for instance the browser, dont like encoded spaces =%20 in file pathes)  // todo: what???
      if(D) Log.i(TAG, "connectedBt remoteBtAddrString="+remoteBtAddrString+" remoteBtNameString="+remoteBtNameString)
      if(remoteBtNameString!=null)
        remoteBtNameString = remoteBtNameString.replaceAll(" ","_")
    }

    try {
      // Get the BluetoothSocket input and output streams
      val mmInStream = if(btSocket!=null) btSocket.getInputStream else null
      val mmOutStream = if(btSocket!=null) btSocket.getOutputStream else null

/*
// todo: attempt to allow nfc-connect on running backup-connection
      if(appService.connectedThread!=null && appService.connectedThread.isRunning) {
        // re-connect a bt connection after it was disconnected
        // todo: but only if a BackupConnection is in use     
        appService.connectedThread.updateStreams(mmInStream, mmOutStream)
        if(D) Log.i(TAG, "connectedBt -> disconnectBackupConnection")
        appService.connectedThread.disconnectBackupConnection

      } else {
*/
        // start the thread to handle the streams
        if(D) Log.i(TAG, "connectedBt, Start ConnectedThread")
        appService.createConnectedThread
        appService.connectedThread.init(mmInStream, mmOutStream, 
                                        myBtAddr, myBtName, 
                                        remoteBtAddrString, remoteBtNameString, 
                                        argMap, () => { 
          // socketCloseFkt to be called by the client
          if(D) Log.i(TAG, "connectedBt disconnecting from "+remoteBtNameString+" "+remoteBtAddrString+" ...")

          // disconnect the bt-socket
          if(btSocket!=null) {
            btSocket.close
            //btSocket=null
          }

          if(D) Log.i(TAG, "connectedBt disconnecting state="+state)
          if(state>RFCommHelperService.STATE_LISTEN) {
            // tell the activity that the connection was lost
            val msg = activityMsgHandler.obtainMessage(RFCommHelperService.DEVICE_DISCONNECT)
            val bundle = new Bundle
            bundle.putString(RFCommHelperService.DEVICE_ADDR, remoteBtAddrString)
            bundle.putString(RFCommHelperService.DEVICE_NAME, remoteBtNameString)
            msg.setData(bundle)
            activityMsgHandler.sendMessage(msg)

            setState(RFCommHelperService.STATE_LISTEN)   // will send MESSAGE_STATE_CHANGE to activity
          }
          if(D) Log.i(TAG, "connectedBt post-ConnectedThread processing done")
        })

        if(D) Log.i(TAG, "connectedBt -> start thread")
        appService.connectedThread.start // -> run() will immediately connect to SocketProxy
        appService.connectedThread.doFirstActor

        // Send the name of the connected device back to the UI Activity
        // note: the main activity may not be active at this moment (but for instance the ConnectPopupActivity)
        if(activityMsgHandler!=null && remoteDevice!=null) {
          val msg = activityMsgHandler.obtainMessage(RFCommHelperService.MESSAGE_DEVICE_NAME)
          val bundle = new Bundle
          bundle.putString(RFCommHelperService.DEVICE_NAME, remoteBtNameString)
          bundle.putString(RFCommHelperService.DEVICE_ADDR, remoteBtAddrString)
          msg.setData(bundle)
          activityMsgHandler.sendMessage(msg)
        }
/*
      }
*/
      //if(D) Log.i(TAG, "connectedBt done")

    } catch {
      case e: IOException =>
        Log.e(TAG, "connectedBt ConnectedThread start temp sockets not created", e)
    }
  }

  // called by ipClientConnectorThread()
  def connectedWifi(socket:java.net.Socket, actor:Boolean, closeDownConnector:() => Unit) :Unit = synchronized {
    if(D) Log.i(TAG, "connectedWifi actor="+actor)

    if(socket!=null) {
      connectedRadio = 2 // wifi

      val remoteSocketAddr = socket.getRemoteSocketAddress.asInstanceOf[InetSocketAddress]
      val remoteWifiAddrString = remoteSocketAddr.getAddress.getHostAddress
      val remoteWifiNameString = remoteSocketAddr.getHostName   // todo: this is not a deviceName, but an ip4-addr
      if(D) Log.i(TAG, "connectedWifi remoteWifiAddrString="+remoteWifiAddrString+" remoteWifiNameString="+remoteWifiNameString)

      // convert spaces to underlines in device name (some android activities, for instance the browser, dont like encoded spaces =%20 in file pathes)
      val myRemoteWifiNameString = remoteWifiNameString.replaceAll(" ","_")
      val localSocketAddr = socket.getLocalSocketAddress.asInstanceOf[InetSocketAddress]
      val localWifiAddrString = localSocketAddr.getAddress.getHostAddress
      val localWifiNameString = localSocketAddr.getHostName

      val mmInStream = socket.getInputStream
      if(mmInStream!=null) {
        val mmOutStream = socket.getOutputStream
        if(mmOutStream!=null) {
/*
// todo: attempt to allow nfc-connect on running backup-connection
          if(appService.connectedThread!=null && appService.connectedThread.isRunning) {
            // re-connect a bt connection after it was disconnected
            // todo: but only if a BackupConnection is in use     
            appService.connectedThread.updateStreams(mmInStream, mmOutStream)
            if(D) Log.i(TAG, "connectedWifi -> disconnectBackupConnection")
            appService.connectedThread.disconnectBackupConnection

          } else {
*/
            appService.createConnectedThread
            appService.connectedThread.init(mmInStream, mmOutStream, localWifiAddrString, localWifiNameString, remoteWifiAddrString, myRemoteWifiNameString, null, () => { 
              // socketCloseFkt to be called by the client
              if(D) Log.i(TAG, "connectedWifi post-ConnectedThread processing remoteWifiAddrString="+remoteWifiAddrString+" myRemoteWifiNameString="+myRemoteWifiNameString)

              // disconnect the wifi-socket
              if(socket!=null) {
                socket.close
                //socket=null
              }

              closeDownConnector() // -> ipClientConnectorThread.closeDownConnector(), close serverSocket + (for wifi-direct: closeDownP2p -> wifiP2pManager.removeGroup())

              if(state>RFCommHelperService.STATE_LISTEN) {
                // tell the activity that the connection was lost
                val msg = activityMsgHandler.obtainMessage(RFCommHelperService.DEVICE_DISCONNECT)
                val bundle = new Bundle
                bundle.putString(RFCommHelperService.DEVICE_ADDR, remoteWifiAddrString)
                bundle.putString(RFCommHelperService.DEVICE_NAME, myRemoteWifiNameString)
                msg.setData(bundle)
                activityMsgHandler.sendMessage(msg)

                setState(RFCommHelperService.STATE_LISTEN)   // will send MESSAGE_STATE_CHANGE to activity
              }
              if(D) Log.i(TAG, "connectedWifi post-ConnectedThread processing done")
            })

            if(D) Log.i(TAG, "connectedWifi -> start thread")
            setState(RFCommHelperService.STATE_CONNECTED)
            appService.connectedThread.start // run() will immediately connect to SocketProxy
            appService.connectedThread.doFirstActor

            // Send the name of the connected device back to the UI Activity
            // note: the main activity may not be active at this moment (but for instance the ConnectPopupActivity)
            if(activityMsgHandler!=null) {
              val msg = activityMsgHandler.obtainMessage(RFCommHelperService.MESSAGE_DEVICE_NAME)
              val bundle = new Bundle
              bundle.putString(RFCommHelperService.DEVICE_NAME, myRemoteWifiNameString)
              bundle.putString(RFCommHelperService.DEVICE_ADDR, remoteWifiAddrString)
              msg.setData(bundle)
              activityMsgHandler.sendMessage(msg)
            }
/*
          }
*/
        }
      }
    }

    if(D) Log.i(TAG, "connectedWifi done")
  }

  def nfcServiceSetup() {
    // this is called by radioDialog/OK, by wifiDirectBroadcastReceiver:WIFI_P2P_THIS_DEVICE_CHANGED_ACTION and by onActivityResult:REQUEST_ENABLE_BT

    if(D) Log.i(TAG, "nfcServiceSetup mNfcAdapter="+mNfcAdapter+" activityResumed="+activityResumed)

    // setup NFC (only for Android 2.3.3+ and only if NFC hardware is available)
    if(android.os.Build.VERSION.SDK_INT>=10 && mNfcAdapter!=null && mNfcAdapter.isEnabled) {
      if(nfcPendingIntent!=null) {
        if(D) Log.i(TAG, "nfcServiceSetup nfcPendingIntent was already set: no enableForegroundDispatch")
      } else {
        if(activity==null || activityRuntimeClass==null) {
          Log.e(TAG, "nfcServiceSetup cannot create nfcPendingIntent activity="+activity+" activityRuntimeClass="+activityRuntimeClass)

        } else if(!activityResumed) {
          Log.e(TAG, "nfcServiceSetup cannot call enableForegroundDispatch while activity is not resumed ############")

        } else {
          // Create a generic PendingIntent that will be delivered to this activity 
          // The NFC stack will fill in the intent with the details of the discovered tag 
          // before delivering to this activity.
          nfcPendingIntent = PendingIntent.getActivity(activity, 0,
                  new Intent(activity, activityRuntimeClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)

          // setup an intent filter for all MIME based dispatches
          val ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
          try {
            if(D) Log.i(TAG, "nfcServiceSetup ndef.addDataType...")
            ndef.addDataType("*/*")   // or "text/plain"
            //ndef.addDataType("application/vnd.tm-nfc")
            if(D) Log.i(TAG, "nfcServiceSetup ndef.addDataType done")
          } catch {
            case e: MalformedMimeTypeException =>
              Log.e(TAG, "nfcServiceSetup ndef.addDataType MalformedMimeTypeException")
              throw new RuntimeException("fail", e)
          }
          nfcFilters = Array(ndef)

          // Setup a tech list for all NfcF tags
          if(D) Log.i(TAG, "nfcServiceSetup setup a tech list for all NfcF tags...")
          nfcTechLists = Array(Array(classOf[NfcF].getName))

          if(D) Log.i(TAG, "nfcServiceSetup enable nfc dispatch mNfcAdapter="+mNfcAdapter+" activity="+activity+
                           " nfcPendingIntent="+nfcPendingIntent+" nfcFilters="+nfcFilters+" nfcTechLists="+nfcTechLists+" ...")
          if(D) Log.i(TAG, "nfcServiceSetup activityResumed="+activityResumed)
          if(activityResumed) {
            AndrTools.runOnUiThread(activity) { () =>
              // This method must be called from the main thread, and only when the activity is in the foreground (resumed). 
              // Also, activities must call disableForegroundDispatch(Activity) before the completion of their onPause callback 
              try {
                mNfcAdapter.enableForegroundDispatch(activity, nfcPendingIntent, nfcFilters, nfcTechLists)
              } catch {
                case illstex:java.lang.IllegalStateException =>
                  // "Foreground dispatch can only be enabled when your activity is resumed"
                  //ignore
                case ex:Exception =>
                  //ignore
              }
              if(D) Log.i(TAG, "nfcServiceSetup enableForegroundDispatch done")
            }
          } else {
            if(D) Log.i(TAG, "nfcServiceSetup skipped enableForegroundDispatch on activityResumed="+activityResumed)
          }
        }
      }

      // embed our btAddress + localP2pWifiAddr in a new NdefMessage to be used via enableForegroundNdefPush
      var nfcString = "app="+appName
      val btAddress = mBluetoothAdapter.getAddress
      if(desiredBluetooth && btAddress!=null) {
        if(nfcString.length>0)
          nfcString += "|"
        nfcString += "bt="+btAddress
      }
      if(desiredWifiDirect && localP2pWifiAddr!=null) {
        if(nfcString.length>0)
          nfcString += "|"
        nfcString += "p2pWifi="+localP2pWifiAddr
      }
      // adding "ip=xx.xx.xx.xx" in case device is connected to wifi-ap (usually this is not the case, so myWifiIpAddr==null is OK)
      val myWifiIpAddr = getWifiIpAddr
      if(myWifiIpAddr!=null) {
        if(nfcString.length>0)
          nfcString += "|"
        nfcString += "ip="+myWifiIpAddr
      }

      if(nfcString.length==0) {
        // this should never happen, right?
        if(D) Log.i(TAG, "nfcServiceSetup nfcString empty")
        nfcForegroundPushMessage=null
        if(activityResumed)
          mNfcAdapter.setNdefPushMessage(null, activity)
          //todo: market error: java.lang.NoSuchMethodError: android.nfc.NfcAdapter.setNdefPushMessage
          //mNfcAdapter.setNdefPushMessage(null, activity, ?)

      } else {
        val ndefRecords = if(android.os.Build.VERSION.SDK_INT>=14)
                            Array(
                              NfcHelper.newTextRecord(nfcString, Locale.ENGLISH, true),
                              android.nfc.NdefRecord.createApplicationRecord(appName)  // if not installed, install from market
                            )
                          else
                            Array(
                              NfcHelper.newTextRecord(nfcString, Locale.ENGLISH, true)
                            )     
        nfcForegroundPushMessage = new NdefMessage(ndefRecords)
        if(nfcForegroundPushMessage!=null) {
          if(activityResumed) {
            mNfcAdapter.setNdefPushMessage(nfcForegroundPushMessage, activity)
            //todo: market error: java.lang.NoSuchMethodError: android.nfc.NfcAdapter.setNdefPushMessage
            //mNfcAdapter.setNdefPushMessage(nfcForegroundPushMessage, activity, ?)
            if(D) Log.i(TAG, "setNdefPushMessage enable nfc ForegroundNdefPush nfcString=["+nfcString+"] done")

          } else {
            if(D) Log.i(TAG, "nfcServiceSetup enable nfc ForegroundNdefPush nfcString=["+nfcString+"] - DELAYED UNTIL activity is resumed ##########")
          }
        }
      }

    } else {
      Log.e(TAG, "nfcServiceSetup NFC NOT set up mNfcAdapter="+mNfcAdapter)
    }
  }

  // called by connectIp() (for ap-wifi sessions, closeDownP2p==null) 
  // and by wifiBroadcastReceiver/CONNECTION_CHANGED_ACTION (wifi-direct sessions, closeDownP2p set)
  def ipClientConnectorThread(isHost:Boolean, inetAddressTarget:java.net.InetAddress, closeDownP2p:() => Unit) = {
    if(D) Log.i(TAG, "ipClientConnectorThread isHost="+isHost+" inetAddressTarget="+inetAddressTarget)

    // start socket communication
    new Thread() {
      override def run() {
        var serverSocket:ServerSocket = null
        var socket:Socket = null

        def closeDownConnector() {
          // this will be called (by both sides) when the thread is finished
          if(D) Log.i(TAG, "ipClientConnectorThread closeDownConnector p2pConnected="+p2pConnected+" p2pChannel="+p2pChannel+" socket="+socket+" serverSocket="+serverSocket)

          if(serverSocket!=null) {
            serverSocket.close
            serverSocket=null
          }

          if(D) Log.i(TAG, "ipClientConnectorThread closeDownConnector -> closeDownP2p="+closeDownP2p+" (null for ap-wifi)")
          if(closeDownP2p!=null) { // will be null for ap-wifi
            closeDownP2p()  // -> wifiP2pManager.removeGroup()
          }
        }

        val port = ipPort // our app-specific rfcomm ip-port
        if(isHost) {
          // which device becomes the isGroupOwner is random, but it will be the device we run our serversocket on...
          // by convention, we make the GroupOwner (using the serverSocket) also the filetransfer-non-actor
          // start server socket
          if(D) Log.i(TAG, "ipClientConnectorThread server: new ServerSocket("+port+")")
          try {
            serverSocket = new ServerSocket(port)
            if(D) Log.i(TAG, "ipClientConnectorThread serverSocket opened")
            socket = serverSocket.accept
            if(socket!=null) {
              connectedWifi(socket, actor=false, closeDownConnector)
            }
          } catch {
            case ioException:IOException =>
              Log.e(TAG, "ipClientConnectorThread serverSocket failed to connect ex="+ioException.getMessage)
              closeDownConnector
          }

        } else {
          // which device becomes the Group client is random, but this is the device we run our client socket on...
          // by convention, we make the Group client (using the client socket) also the filetransfer-actor (will start the delivery)
          // because we are NOT the groupOwner, the groupOwnerAddress is the address of the OTHER device

          // little pause to make sure we (the client) don't try to connect before the socketServer is ready
          try { Thread.sleep(300) } catch { case ex:Exception => }

          val SOCKET_TIMEOUT = 5000
          // we wait up to 5000 ms for the connection...
          val host = inetAddressTarget.getHostAddress
          if(D) Log.i(TAG, "ipClientConnectorThread client: connect to host="+host+" port="+port)
          socket = new Socket()
          try {
            socket.bind(null)
            socket.connect(new InetSocketAddress(host, port), SOCKET_TIMEOUT)
            // if we don't get connected, an ioexception is thrown, otherwise we continue here by connecting to the other peer
            connectedWifi(socket, actor=true, closeDownConnector)
          } catch {
            case ioException:IOException =>
              Log.e(TAG, "ipClientConnectorThread client socket failed to connect ex="+ioException.getMessage+" ########")
              closeDownConnector
          }
        }
      }
    }.start
  }


  def newWiFiDirectBroadcastReceiver() :WiFiDirectBroadcastReceiver = {
    return new WiFiDirectBroadcastReceiver()
  }

  class WiFiDirectBroadcastReceiver() extends BroadcastReceiver {
    private val TAG = "RFCommHelperService WiFiDirectBroadcastReceiver"
    private val D = true

    override def onReceive(activity:Context, intent:Intent) {
      val action = intent.getAction

      if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {                              // p2pWifi has been enabled (or disabled)

        val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
        if(D) Log.i(TAG, "WIFI_P2P_STATE_CHANGED_ACTION state="+state+" ####")
        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
          // Wifi Direct mode is enabled
          //if(D) Log.i(TAG, "WifiP2pEnabled true ####")
          isWifiP2pEnabled=true
          
        } else {
          //if(D) Log.i(TAG, "WifiP2pEnabled false ####")
          p2pConnected = false
          isWifiP2pEnabled=false
        }
        // call mainViewUpdate to update the radio icons shown
        activityMsgHandler.obtainMessage(RFCommHelperService.UI_UPDATE, -1, -1, null).sendToTarget

      } else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {                 // our device now has a p2pWifi mac-addr (or has lost it)
        val wifiP2pDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE).asInstanceOf[WifiP2pDevice]
        if(localP2pWifiAddr==null || localP2pWifiAddr!=wifiP2pDevice.deviceAddress) {
          // we now know our p2p mac address, we now can do nfcServiceSetup
          if(D) Log.i(TAG, "THIS_DEVICE_CHANGED_ACTION OUR deviceName="+wifiP2pDevice.deviceName+" deviceAddress="+wifiP2pDevice.deviceAddress) //+" info="+wifiP2pDevice.toString)
          localP2pWifiAddr = wifiP2pDevice.deviceAddress
          if(D) Log.i(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> nfcServiceSetup")
          nfcServiceSetup
        }

      } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {                      // there is an update for the discovered p2pWifi devices
        if(D) Log.i(TAG, "PEERS_CHANGED_ACTION number of p2p peers changed ####")
        discoveringPeersInProgress = false

        if(wifiP2pManager==null) {
          if(D) Log.i(TAG, "PEERS_CHANGED_ACTION wifiP2pManager==null ####")

        } else {
          //if(D) Log.i(TAG, "PEERS_CHANGED_ACTION requestPeers() ####")
          wifiP2pManager.requestPeers(p2pChannel, new PeerListListener() {
            override def onPeersAvailable(wifiP2pDeviceList:WifiP2pDeviceList) {
              // wifiP2pDeviceList.getDeviceList() is a list of WifiP2pDevice objects, each containg deviceAddress, deviceName, primaryDeviceType, etc.
              //if(D) Log.i(TAG, "onPeersAvailable wifiP2pDeviceList="+wifiP2pDeviceList)
              if(wifiP2pDeviceList!=null) {
                if(D) Log.i(TAG, "onPeersAvailable wifiP2pDeviceList.getDeviceList.size="+wifiP2pDeviceList.getDeviceList.size)
              }
              wifiP2pDeviceArrayList.clear
              wifiP2pDeviceArrayList.addAll(wifiP2pDeviceList.getDeviceList.asInstanceOf[java.util.Collection[WifiP2pDevice]])
              val wifiP2pDeviceListCount = wifiP2pDeviceArrayList.size
              if(D) Log.i(TAG, "onPeersAvailable wifiP2pDeviceListCount="+wifiP2pDeviceListCount)

              if(wifiP2pDeviceListCount>0) {
                // list all peers
                for(i <- 0 until wifiP2pDeviceListCount) {
                  val wifiP2pDevice = wifiP2pDeviceArrayList.get(i)
                  if(wifiP2pDevice != null) {
                    val statusString = if(wifiP2pDevice.status==0) "connected" 
                                  else if(wifiP2pDevice.status==1) "invited" 
                                  else if(wifiP2pDevice.status==2) "failed" 
                                  else if(wifiP2pDevice.status==3) "available" 
                                  else "unknown="+wifiP2pDevice.status
                    if(D) Log.i(TAG, "device "+i+" deviceName="+wifiP2pDevice.deviceName+" deviceAddress="+wifiP2pDevice.deviceAddress+" status="+statusString+" ####")
                    // status: connected=0, invited=1, failed=2, available=3
                   
                    if(p2pWifiDiscoveredCallbackFkt!=null)
                      p2pWifiDiscoveredCallbackFkt(wifiP2pDevice)
                  }
                }
              }
            }
          })
        }

      } else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION==action) {                        // we got p2pWifi client/client connected or disconnected
        // as a result of us (or some other device) calling wifiP2pManager.connect() 

        val networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO).asInstanceOf[NetworkInfo]
        if(D) Log.i(TAG, "CONNECTION_CHANGED_ACTION new p2p-connect-state="+networkInfo.isConnected+" getSubtypeName="+networkInfo.getSubtypeName)

        if(wifiP2pManager==null) {
          if(D) Log.i(TAG, "CONNECTION_CHANGED_ACTION wifiP2pManager==null ###########")
          // need wifiP2pManager to call requestConnectionInfo() to get groupOwnerAddress and isGroupOwner
          return
        }
        
        if(networkInfo.isConnected && p2pConnected) {
          if(D) Log.i(TAG, "CONNECTION_CHANGED_ACTION we are already connected - strange... ignore ###########")
          // todo: new p2p-connect, but we were connected already (probably this is how we set up a group of 3 or more clients?)
          return
        }

        if(!networkInfo.isConnected) {
          if(!p2pConnected) {
            if(D) Log.i(TAG, "CONNECTION_CHANGED_ACTION we are now disconnected, we were disconnect already")
            return
          }
          // we thought we are connected, but now we have been disconnected
          if(D) Log.i(TAG, "CONNECTION_CHANGED_ACTION we are now disconnected, set p2pConnected=false")
          p2pConnected = false
          stopActiveConnection
          return

        } else {
          // we got connected with another device, request connection info to get 
          // the groupOwnerAddress and find out if our device isGroupOwner (-> ServerSocket.accept) or not (-> socket.connect)
          p2pConnected = true
          if(D) Log.i(TAG, "CONNECTION_CHANGED_ACTION we are now p2pWifi connected with the other device")
          wifiP2pManager.requestConnectionInfo(p2pChannel, new WifiP2pManager.ConnectionInfoListener() {
            override def onConnectionInfoAvailable(wifiP2pInfo:WifiP2pInfo) {
              if(D) Log.i(TAG, "CONNECTION_CHANGED_ACTION onConnectionInfoAvailable groupOwnerAddress="+wifiP2pInfo.groupOwnerAddress+" isGroupOwner="+wifiP2pInfo.isGroupOwner+" ###############")

              def closeDownP2p() {
                if(D) Log.i(TAG, "closeDownP2p p2pConnected="+p2pConnected)
                if(p2pConnected) {
                  //if(D) Log.i(TAG, "closeDownP2p wifiP2pManager.removeGroup() (this is how we disconnect from p2pWifi) SKIP ##################")

                  wifiP2pManager.removeGroup(p2pChannel, new ActionListener() {
                    override def onSuccess() {
                      if(D) Log.i(TAG, "closeDownP2p wifiP2pManager.removeGroup() success")
                      // wifiDirectBroadcastReceiver will notify us
                    }

                    override def onFailure(reason:Int) {
                      if(D) Log.i(TAG, "closeDownP2p wifiP2pManager.removeGroup() failed reason="+reason+" ############")
                      // reason ERROR=0, P2P_UNSUPPORTED=1, BUSY=2
                      // note: it seems to be 'normal' for one of the two devices to receive reason=2 on disconenct
                    }
                  })
                }
              }

              // now start a ServerSocket thread (if isGroupOwner) or a client socket thread (if not isGroupOwner)
              // then call connectedWifi(), which will open input- and output streams and start appService.connectedThread.start
              ipClientConnectorThread(wifiP2pInfo.isGroupOwner, wifiP2pInfo.groupOwnerAddress, closeDownP2p)
            }
          })
        }
      }
    }
  }
}

