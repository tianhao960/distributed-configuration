package mars.config.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j;
import mars.config.request.ListenConfigRequest;
import mars.config.request.response.ListenConfigResponse;
import mars.config.util.ErrorCode;
import org.joda.time.DateTime;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;

@Log4j
public class ConfigClientConnection implements Runnable{


    private Map<String, ListenConfigResponse> requestResponseMap;

    private ConcurrentHashMap<String, Set<ConfigClientWatcher>> watchers;

    private ConfigClient client;

    private SocketChannel sc;

    private BlockingQueue<ListenConfigRequest> requests;

    private SelectionKey selectionKey;

    private Selector selector;

    private Thread thread;

    /**
     * This buffer is only used to read the length of the incoming message.
     */
    private final ByteBuffer lenBuffer = ByteBuffer.allocateDirect(4);

    /**
     * After the length is read, a new incomingBuffer is allocated in
     * readLength() to receive the full message.
     */
    protected ByteBuffer incomingBuffer = lenBuffer;


    public ConfigClientConnection(ConfigClient client){
        this.client = client;
        requestResponseMap = new ConcurrentHashMap<>();
        watchers = new ConcurrentHashMap<>();
        requests = new LinkedBlockingQueue<>();
        connect();
        thread = new Thread(this,"config client:"+client.getHost()+","+client.getListenPort());
        thread.start();
    }

    private void connect(){
        try{
            this.sc = SocketChannel.open();
            sc.configureBlocking(false);
            //sc.bind(new InetSocketAddress(client.getHost(),client.getPort()));
            selector = Selector.open();
            selectionKey = sc.register(selector, SelectionKey.OP_CONNECT);
            sc.connect(new InetSocketAddress(client.getHost(),client.getListenPort()));
            log.info("connecting to server...");
            while (!sc.finishConnect()){
                if(log.isDebugEnabled()){
                    log.debug("connecting to server...");
                }

                TimeUnit.MILLISECONDS.sleep(100);
            }
            log.info("connect to server success.");
            selectionKey.interestOps(SelectionKey.OP_READ|SelectionKey.OP_WRITE);
        }catch (Exception e){
            log.error("connect to server fail.");
            try {
                sc.close();
            } catch (IOException e1) {
                log.error("close socket fail.",e1);
            }
            throw  new RuntimeException("connect to server fail.",e);
        }
    }

    public void sendRequest(ListenConfigRequest request){
        synchronized (requests){
            if(request.getIsListenEvent()){
                if(request.getWatcher()!=null){
                    StringBuilder path = new StringBuilder("/");
                    path.append(request.getAppgroup()).append("/");
                    path.append(request.getApp()).append("/");
                    path.append(request.getKey());
                    if(watchers.containsKey(request.getId())){
                        watchers.get(path.toString()).add(request.getWatcher());
                    }else{
                        Set<ConfigClientWatcher> temp = new CopyOnWriteArraySet<>();
                        temp.add(request.getWatcher());
                        watchers.put(path.toString(),temp);
                    }
                }else {
                    throw new IllegalArgumentException("miss watcher for request "+request.toString());
                }
            }
            requests.offer(request);
            selectionKey.interestOps(selectionKey.interestOps()|SelectionKey.OP_WRITE);
        }
    }

    public ListenConfigResponse getResponse(ListenConfigRequest request,Integer seconds){
        DateTime dt = DateTime.now();
        dt = dt.plusSeconds(seconds);
        Date end = dt.toDate();
        while (requestResponseMap.get(request.getId())==null){
            Date now = new Date();
            if(now.after(end)){
                break;
            }
        }
        ListenConfigResponse response;
        response = requestResponseMap.get(request.getId());
        if(response == null){
            response = new ListenConfigResponse();
            response.setIsSuccess(false);
            response.setErrorCode(ErrorCode.TIMEOUT_ERROR);
        }else{
            requestResponseMap.remove(request.getId());
        }
        return response;
    }


    private void readResponse(SelectionKey key){
        try{
            SocketChannel sc = (SocketChannel)key.channel();
            if(!sc.isOpen()){
                log.info("socket closed");
                return;
            }
            int count = sc.read(incomingBuffer);
            if(count<0){
                //check EOF
                closeSock();
            }
            //this is because you can't assume how much data you will read
            if (!incomingBuffer.hasRemaining()) {
                incomingBuffer.flip();
                if (incomingBuffer == lenBuffer) {
                    int len = incomingBuffer.getInt();
                    incomingBuffer = ByteBuffer.allocate(len);
                }else{
                    ObjectMapper om = new ObjectMapper();
                    ListenConfigResponse response = om.readValue(incomingBuffer.array(),ListenConfigResponse.class);
                    if(response.getId().equals("-1")){
                        //ping response
                    }else if(response.getId().equals("-2")){
                        //data change event
                        ConfigEvent configEvent = response.getConfigEvent();

                        Collection<ConfigClientWatcher> pathWatchers = watchers.get(configEvent.getPath());
                        if(pathWatchers!=null){
                            for(ConfigClientWatcher pathWatcher : pathWatchers){
                                pathWatcher.process(configEvent);
                            }
                        }
                    }else{
                        requestResponseMap.put(response.getId(),response);
                    }
                    lenBuffer.clear();
                    incomingBuffer = lenBuffer;
                }
            }
        }catch (Exception e){
            log.error("process key "+key.toString()+" fail.",e);
        }
    }

    private void sendRequest(SelectionKey key){
        try{
            SocketChannel sc = (SocketChannel)key.channel();
            if(!sc.isOpen()){
                log.info("socket closed");
                return;
            }
            synchronized (requests){
                ListenConfigRequest request;
                while ((request = requests.poll(100,TimeUnit.MILLISECONDS))!=null){
                    ObjectMapper om = new ObjectMapper();
                    byte[] bytes = om.writeValueAsBytes(request);
                    Integer len = bytes.length;
                    ByteBuffer body = ByteBuffer.allocate(len+4);
                    body.putInt(len);
                    body.put(bytes);
                    body.flip();
                    sc.write(body);
                }
                if(requests.size()>0){
                    selectionKey.interestOps(selectionKey.interestOps()|SelectionKey.OP_WRITE);
                }else{
                    selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                }
            }
        }catch (Exception e){
            log.error("process key "+key.toString()+" fail.",e);
        }
    }

    public String getRequestId(){
        return  sc.socket().getInetAddress().toString()+":"+ UUID.randomUUID();
    }

    @Override
    public void run() {
        while (sc.isOpen()){
            try{
                selector.select(1000);
                Set<SelectionKey> selected;
                synchronized (this) {
                    selected = selector.selectedKeys();
                }

                for (SelectionKey k : selected) {
                    if (k.isReadable()) {
                        readResponse(k);
                    }else if(k.isWritable()){
                        sendRequest(k);
                    }else{
                        log.warn("unhandled event:"+k.toString());
                    }
                }
                selected.clear();
            }catch (Exception e){
                if(!sc.isOpen()){
                    log.info("socket closed.");
                }else{
                    log.error("unhandled exception.",e);
                }

            }
        }
    }

    public void closeSock() {
        if (sc.isOpen() == false) {
            return;
        }

        try {
            /*
             * The following sequence of code is stupid! You would think that
             * only sock.close() is needed, but alas, it doesn't work that way.
             * If you just do sock.close() there are cases where the socket
             * doesn't actually close...
             */
            sc.socket().shutdownOutput();
        } catch (IOException e) {
            // This is a relatively common exception that we can't avoid
            if (log.isDebugEnabled()) {
                log.debug("ignoring exception during output shutdown", e);
            }
        }
        try {
            sc.socket().shutdownInput();
        } catch (IOException e) {
            // This is a relatively common exception that we can't avoid
            if (log.isDebugEnabled()) {
                log.debug("ignoring exception during input shutdown", e);
            }
        }
        try {
            sc.socket().close();
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("ignoring exception during socket close", e);
            }
        }
        try {
            sc.close();
            // XXX The next line doesn't seem to be needed, but some posts
            // to forums suggest that it is needed. Keep in mind if errors in
            // this section arise.
            // factory.selector.wakeup();
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("ignoring exception during socketchannel close", e);
            }
        }

        if(selectionKey!=null){
            try {
                // need to cancel this selection key from the selector
                selectionKey.cancel();
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("ignoring exception during selectionkey cancel", e);
                }
            }
        }
    }
}
