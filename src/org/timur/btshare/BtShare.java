// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: BtShare.proto

package org.timur.btshare;

public final class BtShare {
  private BtShare() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }
  public static final class Message extends
      com.google.protobuf.GeneratedMessageLite {
    // Use Message.newBuilder() to construct.
    private Message() {
      initFields();
    }
    private Message(boolean noInit) {}
    
    private static final Message defaultInstance;
    public static Message getDefaultInstance() {
      return defaultInstance;
    }
    
    public Message getDefaultInstanceForType() {
      return defaultInstance;
    }
    
    // required string command = 1;
    public static final int COMMAND_FIELD_NUMBER = 1;
    private boolean hasCommand;
    private java.lang.String command_ = "";
    public boolean hasCommand() { return hasCommand; }
    public java.lang.String getCommand() { return command_; }
    
    // required string fromName = 20;
    public static final int FROMNAME_FIELD_NUMBER = 20;
    private boolean hasFromName;
    private java.lang.String fromName_ = "";
    public boolean hasFromName() { return hasFromName; }
    public java.lang.String getFromName() { return fromName_; }
    
    // required string fromAddr = 21;
    public static final int FROMADDR_FIELD_NUMBER = 21;
    private boolean hasFromAddr;
    private java.lang.String fromAddr_ = "";
    public boolean hasFromAddr() { return hasFromAddr; }
    public java.lang.String getFromAddr() { return fromAddr_; }
    
    // required int64 argCount = 2;
    public static final int ARGCOUNT_FIELD_NUMBER = 2;
    private boolean hasArgCount;
    private long argCount_ = 0L;
    public boolean hasArgCount() { return hasArgCount; }
    public long getArgCount() { return argCount_; }
    
    // optional int64 id = 8;
    public static final int ID_FIELD_NUMBER = 8;
    private boolean hasId;
    private long id_ = 0L;
    public boolean hasId() { return hasId; }
    public long getId() { return id_; }
    
    // optional int64 dataLength = 9;
    public static final int DATALENGTH_FIELD_NUMBER = 9;
    private boolean hasDataLength;
    private long dataLength_ = 0L;
    public boolean hasDataLength() { return hasDataLength; }
    public long getDataLength() { return dataLength_; }
    
    // optional string arg1 = 3;
    public static final int ARG1_FIELD_NUMBER = 3;
    private boolean hasArg1;
    private java.lang.String arg1_ = "";
    public boolean hasArg1() { return hasArg1; }
    public java.lang.String getArg1() { return arg1_; }
    
    // optional string arg2 = 4;
    public static final int ARG2_FIELD_NUMBER = 4;
    private boolean hasArg2;
    private java.lang.String arg2_ = "";
    public boolean hasArg2() { return hasArg2; }
    public java.lang.String getArg2() { return arg2_; }
    
    // optional string arg3 = 5;
    public static final int ARG3_FIELD_NUMBER = 5;
    private boolean hasArg3;
    private java.lang.String arg3_ = "";
    public boolean hasArg3() { return hasArg3; }
    public java.lang.String getArg3() { return arg3_; }
    
    // optional string arg4 = 6;
    public static final int ARG4_FIELD_NUMBER = 6;
    private boolean hasArg4;
    private java.lang.String arg4_ = "";
    public boolean hasArg4() { return hasArg4; }
    public java.lang.String getArg4() { return arg4_; }
    
    // optional bytes argBytes = 7;
    public static final int ARGBYTES_FIELD_NUMBER = 7;
    private boolean hasArgBytes;
    private com.google.protobuf.ByteString argBytes_ = com.google.protobuf.ByteString.EMPTY;
    public boolean hasArgBytes() { return hasArgBytes; }
    public com.google.protobuf.ByteString getArgBytes() { return argBytes_; }
    
    // optional string toName = 22;
    public static final int TONAME_FIELD_NUMBER = 22;
    private boolean hasToName;
    private java.lang.String toName_ = "";
    public boolean hasToName() { return hasToName; }
    public java.lang.String getToName() { return toName_; }
    
    // optional string toAddr = 23;
    public static final int TOADDR_FIELD_NUMBER = 23;
    private boolean hasToAddr;
    private java.lang.String toAddr_ = "";
    public boolean hasToAddr() { return hasToAddr; }
    public java.lang.String getToAddr() { return toAddr_; }
    
    private void initFields() {
    }
    public final boolean isInitialized() {
      if (!hasCommand) return false;
      if (!hasFromName) return false;
      if (!hasFromAddr) return false;
      if (!hasArgCount) return false;
      return true;
    }
    
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (hasCommand()) {
        output.writeString(1, getCommand());
      }
      if (hasArgCount()) {
        output.writeInt64(2, getArgCount());
      }
      if (hasArg1()) {
        output.writeString(3, getArg1());
      }
      if (hasArg2()) {
        output.writeString(4, getArg2());
      }
      if (hasArg3()) {
        output.writeString(5, getArg3());
      }
      if (hasArg4()) {
        output.writeString(6, getArg4());
      }
      if (hasArgBytes()) {
        output.writeBytes(7, getArgBytes());
      }
      if (hasId()) {
        output.writeInt64(8, getId());
      }
      if (hasDataLength()) {
        output.writeInt64(9, getDataLength());
      }
      if (hasFromName()) {
        output.writeString(20, getFromName());
      }
      if (hasFromAddr()) {
        output.writeString(21, getFromAddr());
      }
      if (hasToName()) {
        output.writeString(22, getToName());
      }
      if (hasToAddr()) {
        output.writeString(23, getToAddr());
      }
    }
    
    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;
    
      size = 0;
      if (hasCommand()) {
        size += com.google.protobuf.CodedOutputStream
          .computeStringSize(1, getCommand());
      }
      if (hasArgCount()) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt64Size(2, getArgCount());
      }
      if (hasArg1()) {
        size += com.google.protobuf.CodedOutputStream
          .computeStringSize(3, getArg1());
      }
      if (hasArg2()) {
        size += com.google.protobuf.CodedOutputStream
          .computeStringSize(4, getArg2());
      }
      if (hasArg3()) {
        size += com.google.protobuf.CodedOutputStream
          .computeStringSize(5, getArg3());
      }
      if (hasArg4()) {
        size += com.google.protobuf.CodedOutputStream
          .computeStringSize(6, getArg4());
      }
      if (hasArgBytes()) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(7, getArgBytes());
      }
      if (hasId()) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt64Size(8, getId());
      }
      if (hasDataLength()) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt64Size(9, getDataLength());
      }
      if (hasFromName()) {
        size += com.google.protobuf.CodedOutputStream
          .computeStringSize(20, getFromName());
      }
      if (hasFromAddr()) {
        size += com.google.protobuf.CodedOutputStream
          .computeStringSize(21, getFromAddr());
      }
      if (hasToName()) {
        size += com.google.protobuf.CodedOutputStream
          .computeStringSize(22, getToName());
      }
      if (hasToAddr()) {
        size += com.google.protobuf.CodedOutputStream
          .computeStringSize(23, getToAddr());
      }
      memoizedSerializedSize = size;
      return size;
    }
    
    public static org.timur.btshare.BtShare.Message parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static org.timur.btshare.BtShare.Message parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static org.timur.btshare.BtShare.Message parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static org.timur.btshare.BtShare.Message parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static org.timur.btshare.BtShare.Message parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static org.timur.btshare.BtShare.Message parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    public static org.timur.btshare.BtShare.Message parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static org.timur.btshare.BtShare.Message parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input, extensionRegistry)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static org.timur.btshare.BtShare.Message parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static org.timur.btshare.BtShare.Message parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    
    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(org.timur.btshare.BtShare.Message prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }
    
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageLite.Builder<
          org.timur.btshare.BtShare.Message, Builder> {
      private org.timur.btshare.BtShare.Message result;
      
      // Construct using org.timur.btshare.BtShare.Message.newBuilder()
      private Builder() {}
      
      private static Builder create() {
        Builder builder = new Builder();
        builder.result = new org.timur.btshare.BtShare.Message();
        return builder;
      }
      
      protected org.timur.btshare.BtShare.Message internalGetResult() {
        return result;
      }
      
      public Builder clear() {
        if (result == null) {
          throw new IllegalStateException(
            "Cannot call clear() after build().");
        }
        result = new org.timur.btshare.BtShare.Message();
        return this;
      }
      
      public Builder clone() {
        return create().mergeFrom(result);
      }
      
      public org.timur.btshare.BtShare.Message getDefaultInstanceForType() {
        return org.timur.btshare.BtShare.Message.getDefaultInstance();
      }
      
      public boolean isInitialized() {
        return result.isInitialized();
      }
      public org.timur.btshare.BtShare.Message build() {
        if (result != null && !isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return buildPartial();
      }
      
      private org.timur.btshare.BtShare.Message buildParsed()
          throws com.google.protobuf.InvalidProtocolBufferException {
        if (!isInitialized()) {
          throw newUninitializedMessageException(
            result).asInvalidProtocolBufferException();
        }
        return buildPartial();
      }
      
      public org.timur.btshare.BtShare.Message buildPartial() {
        if (result == null) {
          throw new IllegalStateException(
            "build() has already been called on this Builder.");
        }
        org.timur.btshare.BtShare.Message returnMe = result;
        result = null;
        return returnMe;
      }
      
      public Builder mergeFrom(org.timur.btshare.BtShare.Message other) {
        if (other == org.timur.btshare.BtShare.Message.getDefaultInstance()) return this;
        if (other.hasCommand()) {
          setCommand(other.getCommand());
        }
        if (other.hasFromName()) {
          setFromName(other.getFromName());
        }
        if (other.hasFromAddr()) {
          setFromAddr(other.getFromAddr());
        }
        if (other.hasArgCount()) {
          setArgCount(other.getArgCount());
        }
        if (other.hasId()) {
          setId(other.getId());
        }
        if (other.hasDataLength()) {
          setDataLength(other.getDataLength());
        }
        if (other.hasArg1()) {
          setArg1(other.getArg1());
        }
        if (other.hasArg2()) {
          setArg2(other.getArg2());
        }
        if (other.hasArg3()) {
          setArg3(other.getArg3());
        }
        if (other.hasArg4()) {
          setArg4(other.getArg4());
        }
        if (other.hasArgBytes()) {
          setArgBytes(other.getArgBytes());
        }
        if (other.hasToName()) {
          setToName(other.getToName());
        }
        if (other.hasToAddr()) {
          setToAddr(other.getToAddr());
        }
        return this;
      }
      
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        while (true) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              return this;
            default: {
              if (!parseUnknownField(input, extensionRegistry, tag)) {
                return this;
              }
              break;
            }
            case 10: {
              setCommand(input.readString());
              break;
            }
            case 16: {
              setArgCount(input.readInt64());
              break;
            }
            case 26: {
              setArg1(input.readString());
              break;
            }
            case 34: {
              setArg2(input.readString());
              break;
            }
            case 42: {
              setArg3(input.readString());
              break;
            }
            case 50: {
              setArg4(input.readString());
              break;
            }
            case 58: {
              setArgBytes(input.readBytes());
              break;
            }
            case 64: {
              setId(input.readInt64());
              break;
            }
            case 72: {
              setDataLength(input.readInt64());
              break;
            }
            case 162: {
              setFromName(input.readString());
              break;
            }
            case 170: {
              setFromAddr(input.readString());
              break;
            }
            case 178: {
              setToName(input.readString());
              break;
            }
            case 186: {
              setToAddr(input.readString());
              break;
            }
          }
        }
      }
      
      
      // required string command = 1;
      public boolean hasCommand() {
        return result.hasCommand();
      }
      public java.lang.String getCommand() {
        return result.getCommand();
      }
      public Builder setCommand(java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasCommand = true;
        result.command_ = value;
        return this;
      }
      public Builder clearCommand() {
        result.hasCommand = false;
        result.command_ = getDefaultInstance().getCommand();
        return this;
      }
      
      // required string fromName = 20;
      public boolean hasFromName() {
        return result.hasFromName();
      }
      public java.lang.String getFromName() {
        return result.getFromName();
      }
      public Builder setFromName(java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasFromName = true;
        result.fromName_ = value;
        return this;
      }
      public Builder clearFromName() {
        result.hasFromName = false;
        result.fromName_ = getDefaultInstance().getFromName();
        return this;
      }
      
      // required string fromAddr = 21;
      public boolean hasFromAddr() {
        return result.hasFromAddr();
      }
      public java.lang.String getFromAddr() {
        return result.getFromAddr();
      }
      public Builder setFromAddr(java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasFromAddr = true;
        result.fromAddr_ = value;
        return this;
      }
      public Builder clearFromAddr() {
        result.hasFromAddr = false;
        result.fromAddr_ = getDefaultInstance().getFromAddr();
        return this;
      }
      
      // required int64 argCount = 2;
      public boolean hasArgCount() {
        return result.hasArgCount();
      }
      public long getArgCount() {
        return result.getArgCount();
      }
      public Builder setArgCount(long value) {
        result.hasArgCount = true;
        result.argCount_ = value;
        return this;
      }
      public Builder clearArgCount() {
        result.hasArgCount = false;
        result.argCount_ = 0L;
        return this;
      }
      
      // optional int64 id = 8;
      public boolean hasId() {
        return result.hasId();
      }
      public long getId() {
        return result.getId();
      }
      public Builder setId(long value) {
        result.hasId = true;
        result.id_ = value;
        return this;
      }
      public Builder clearId() {
        result.hasId = false;
        result.id_ = 0L;
        return this;
      }
      
      // optional int64 dataLength = 9;
      public boolean hasDataLength() {
        return result.hasDataLength();
      }
      public long getDataLength() {
        return result.getDataLength();
      }
      public Builder setDataLength(long value) {
        result.hasDataLength = true;
        result.dataLength_ = value;
        return this;
      }
      public Builder clearDataLength() {
        result.hasDataLength = false;
        result.dataLength_ = 0L;
        return this;
      }
      
      // optional string arg1 = 3;
      public boolean hasArg1() {
        return result.hasArg1();
      }
      public java.lang.String getArg1() {
        return result.getArg1();
      }
      public Builder setArg1(java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasArg1 = true;
        result.arg1_ = value;
        return this;
      }
      public Builder clearArg1() {
        result.hasArg1 = false;
        result.arg1_ = getDefaultInstance().getArg1();
        return this;
      }
      
      // optional string arg2 = 4;
      public boolean hasArg2() {
        return result.hasArg2();
      }
      public java.lang.String getArg2() {
        return result.getArg2();
      }
      public Builder setArg2(java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasArg2 = true;
        result.arg2_ = value;
        return this;
      }
      public Builder clearArg2() {
        result.hasArg2 = false;
        result.arg2_ = getDefaultInstance().getArg2();
        return this;
      }
      
      // optional string arg3 = 5;
      public boolean hasArg3() {
        return result.hasArg3();
      }
      public java.lang.String getArg3() {
        return result.getArg3();
      }
      public Builder setArg3(java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasArg3 = true;
        result.arg3_ = value;
        return this;
      }
      public Builder clearArg3() {
        result.hasArg3 = false;
        result.arg3_ = getDefaultInstance().getArg3();
        return this;
      }
      
      // optional string arg4 = 6;
      public boolean hasArg4() {
        return result.hasArg4();
      }
      public java.lang.String getArg4() {
        return result.getArg4();
      }
      public Builder setArg4(java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasArg4 = true;
        result.arg4_ = value;
        return this;
      }
      public Builder clearArg4() {
        result.hasArg4 = false;
        result.arg4_ = getDefaultInstance().getArg4();
        return this;
      }
      
      // optional bytes argBytes = 7;
      public boolean hasArgBytes() {
        return result.hasArgBytes();
      }
      public com.google.protobuf.ByteString getArgBytes() {
        return result.getArgBytes();
      }
      public Builder setArgBytes(com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasArgBytes = true;
        result.argBytes_ = value;
        return this;
      }
      public Builder clearArgBytes() {
        result.hasArgBytes = false;
        result.argBytes_ = getDefaultInstance().getArgBytes();
        return this;
      }
      
      // optional string toName = 22;
      public boolean hasToName() {
        return result.hasToName();
      }
      public java.lang.String getToName() {
        return result.getToName();
      }
      public Builder setToName(java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasToName = true;
        result.toName_ = value;
        return this;
      }
      public Builder clearToName() {
        result.hasToName = false;
        result.toName_ = getDefaultInstance().getToName();
        return this;
      }
      
      // optional string toAddr = 23;
      public boolean hasToAddr() {
        return result.hasToAddr();
      }
      public java.lang.String getToAddr() {
        return result.getToAddr();
      }
      public Builder setToAddr(java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasToAddr = true;
        result.toAddr_ = value;
        return this;
      }
      public Builder clearToAddr() {
        result.hasToAddr = false;
        result.toAddr_ = getDefaultInstance().getToAddr();
        return this;
      }
      
      // @@protoc_insertion_point(builder_scope:btshare.Message)
    }
    
    static {
      defaultInstance = new Message(true);
      org.timur.btshare.BtShare.internalForceInit();
      defaultInstance.initFields();
    }
    
    // @@protoc_insertion_point(class_scope:btshare.Message)
  }
  
  
  static {
  }
  
  public static void internalForceInit() {}
  
  // @@protoc_insertion_point(outer_class_scope)
}
