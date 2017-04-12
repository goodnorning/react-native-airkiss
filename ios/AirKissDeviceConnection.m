

#import "AirKissDeviceConnection.h"
#import "AirKissEncoder.h"
#import "GCDAsyncUdpSocket.h"

#define kAirKiss_Port                    12476
#define AIRKISS_LAN_SSDP_NOTIFY_CMD     0x1002
#define kMagic_Num_0             0xFD
#define kMagic_Num_1             0x01
#define kMagic_Num_2             0xFE
#define kMagic_Num_3             0xFC
@interface AirKissConnectionD()<GCDAsyncUdpSocketDelegate>
{
    NSTimer           *_timer;          // 超过1分钟未连接成功则表示失败
    GCDAsyncUdpSocket *_serverUdpSocket;
    long              _tag;
    BOOL              _gotDeviceData;
}
@end

@implementation AirKissConnectionD

//- (instancetype)init
//{
//    self = [super init];
//    if (self) {
//        _tag             = 0;
//        _connectionDone  = false;
//    
//        [self setupServerUdpSocket];
//    }
//    return self;
//}

#pragma mark - Connection
- (void)connectAirKissDevice {

    _tag                      = 0;
    _gotDeviceData            = false;
    _timer =nil;
    _serverUdpSocket = nil;
    [self setupServerUdpSocket];
    //超过30S没有获取到信息，则结束
    dispatch_async(dispatch_get_main_queue(), ^{
        _timer = [NSTimer scheduledTimerWithTimeInterval:30
                                                  target:self
                                                selector:@selector(getDeviceInfoFailure)
                                                userInfo:nil
                                                 repeats:NO];
    });
}

- (void)closeConnection {
    _gotDeviceData = true;
    
    if (_timer) {
        [_timer invalidate];
        _timer = nil;        
    }
    if (_serverUdpSocket) {
        [_serverUdpSocket close];
        _serverUdpSocket = nil;      
    }
}


- (void)setupServerUdpSocket {
    if (!_serverUdpSocket) {
        _serverUdpSocket = [[GCDAsyncUdpSocket alloc] initWithDelegate:self delegateQueue:dispatch_get_main_queue()];
        [_serverUdpSocket enableBroadcast:YES error:nil];
    }
    
    NSError *error = nil;
    
    if (![_serverUdpSocket bindToPort:kAirKiss_Port error:&error])
    {
        return;
    }
    
    if (![_serverUdpSocket beginReceiving:&error])
    {
        return;
    }
}

#pragma mark - Event Response
- (void)getDeviceInfoFailure {
    NSLog(@"timeout------------------------");
    if (!_gotDeviceData) {
        [self closeConnection];
        if (_connectionFailure) {
            _connectionFailure();
        }       
    }
}

#pragma mark - GCDAsyncUdpSocketDelegate
- (void)udpSocket:(GCDAsyncUdpSocket *)sock didSendDataWithTag:(long)tag
{
    // You could add checks here
}

- (void)udpSocket:(GCDAsyncUdpSocket *)sock didNotSendDataWithTag:(long)tag dueToError:(NSError *)error
{
    // You could add checks here
}

- (void)udpSocket:(GCDAsyncUdpSocket *)sock
   didReceiveData:(NSData *)data
      fromAddress:(NSData *)address
withFilterContext:(id)filterContext
{
    if (_serverUdpSocket == sock) {
        if (_gotDeviceData) {
            return;
        }

        Byte *bytes = (Byte *) [data bytes];
        NSString *ret = [self parseNotifyData:bytes length:(int)data.length];
        if (ret) {
            _gotDeviceData = true;
            [self closeConnection];
            if (_connectionSuccess) {
                _connectionSuccess(ret);
            }
        }
    }
}
-  (NSString*)parseNotifyData:(UInt8*)data length:(int)length {
    // NSLog(@"parseNotifyData,length=%d",length);
    Byte *newData = (Byte*)malloc(length);
    int count = 0;
    BOOL flag = false;
    while(count<length-4 && length<200){
        Byte num0 = data[count++];
        Byte num1 = data[count++];
        Byte num2 = data[count++];
        Byte num3 = data[count++];
        // NSLog(@"parseNotifyData,num0=0x%x,num1=0x%x,num2=0x%x,num3=0x%x",num0,num1,num2,num3);
        if (num0 == kMagic_Num_0 && num1 == kMagic_Num_1 && num2 == kMagic_Num_2 && num3 == kMagic_Num_3) {
            // NSLog(@"-----------find magic num");
            for (int i=0;i<length-count;i++){
                newData[i] = data[count+i];
            }
            flag = true;
            break;
        }
    }
    
    if (flag) {
        count = 0;
        length = length-count;
        while(count<length-2&&length>2) {
            uint16_t cmd = newData[count++];
            cmd <<= 8;
            cmd |= newData[count++];
            if(cmd == AIRKISS_LAN_SSDP_NOTIFY_CMD) {
                Byte *buffer = (Byte*)malloc(length);
                memset(buffer,0,length);
                int len = length-count;
                for (int i=0;i<len;i++) {
                    buffer[i] = newData[i+count];
                }
                NSData *aData = [[NSData alloc] initWithBytes:buffer length:len];
                NSString *aString = [[NSString alloc] initWithData:aData encoding:NSUTF8StringEncoding];
                // NSLog(@"parseNotifyData------%@",aString);
                free(newData);
                free(buffer);
                return aString;
            }
        }
    }
    free(newData);
    return nil;
}

@end
