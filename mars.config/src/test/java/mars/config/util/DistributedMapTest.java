package mars.config.util;

import mars.config.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class DistributedMapTest {

    @Autowired
    private ZooKeeperUtil zooKeeperUtil;

    @Test
    public void testPut() throws IOException {
        DistributedMap<String,String> testMap = new DistributedMap<>(zooKeeperUtil.getClient());
        testMap.put("testmap2","test");
        String value = testMap.get("testmap2");
        Assert.isTrue(value.equals("test"));
        testMap.close();

        DistributedMap<String,ArrayList<String>> listenMap = DistributedMap.getListenMap(zooKeeperUtil.getClient());
        listenMap.get("/config/db/host");
    }
}
