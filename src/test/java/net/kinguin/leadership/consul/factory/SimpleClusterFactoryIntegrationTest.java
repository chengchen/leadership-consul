package net.kinguin.leadership.consul.factory;

import com.palantir.docker.compose.DockerComposeRule;
import net.kinguin.leadership.consul.IntegrationTest;
import net.kinguin.leadership.consul.config.ClusterConfiguration;
import net.kinguin.leadership.consul.election.Gambler;
import net.kinguin.leadership.consul.election.Info;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

@Category(IntegrationTest.class)
public class SimpleClusterFactoryIntegrationTest {
    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
        .file("src/test/resources/docker-compose.yml")
        .build();

    @Test
    public void should_elect_leader_for_the_first_time() throws Exception {
        ClusterConfiguration config = new ClusterConfiguration();
        config.getConsul().setHost("localhost");
        config.getConsul().setPort(9500);
        config.getSession().setRefresh(1);
        config.getElection().setDelay(3);
        config.getElection().setFrequency(1);

        CountDownLatch latch = new CountDownLatch(2);

        new SimpleClusterFactory()
            .mode(SimpleClusterFactory.MODE_MULTI)
            .configure(config)
            .debug(true)
            .build()
            .asObservable()
            .subscribe(i -> {
                Info info = (Info) i;

                if (Gambler.ELECTED_FIRST_TIME.equals(info.status) || Gambler.ELECTED.equals(info.status)) {
                    latch.countDown();
                }
            });

        latch.await(5, TimeUnit.SECONDS);

        assertEquals(0, latch.getCount());
    }
}