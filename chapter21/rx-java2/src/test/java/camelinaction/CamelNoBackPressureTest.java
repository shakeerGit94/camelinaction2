package camelinaction;

import io.reactivex.Flowable;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreams;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.reactivestreams.Publisher;

public class CamelNoBackPressureTest extends CamelTestSupport {

    @Test
    public void testNoBackPressure() throws Exception {
        CamelReactiveStreamsService rxCamel = CamelReactiveStreams.get(context);

        // create a published that receive from the inbox stream
        Publisher<String> numbers = rxCamel.fromStream("inbox", String.class);

        // use stream engine to subscribe from the publisher
        Flowable.fromPublisher(numbers)
            .doOnNext(c -> {
                log.info("Processing message {}", c);
                Thread.sleep(1000);
            })
            .subscribe();

        // send in 200 messages
        log.info("Sending 200 messages ...");
        for (int i = 0; i < 200; i++) {
            fluentTemplate.withBody("Hello " + i).to("seda:inbox?waitForTaskToComplete=Never").send();
        }
        log.info("Sent 200 messages done");

        // let it run for 250 seconds
        Thread.sleep(250 * 1000L);
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("seda:inbox")
                    // use a little delay as otherwise Camel is to fast and the inflight throttler cannot react so precisely
                    // and it also spread the incoming messages more evenly than a big burst
                    .delay(100)
                    .log("Camel routing to Reactive Streams: ${body}")
                    .to("reactive-streams:inbox");
            }
        };
    }
}
