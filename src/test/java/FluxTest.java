import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

@Slf4j
public class FluxTest {

    @Test
    public void fluxSubscriber() {
        Flux<String> fluxString = Flux.just("Vinicius", "Alana", "Valdir", "Ana").log();

        StepVerifier.create(fluxString)
                .expectNext("Vinicius", "Alana", "Valdir", "Ana")
                .verifyComplete();

    }

    @Test
    public void fluxSubscriberNumbers() {
        Flux<Integer> fluxString = Flux.range(1, 5).log();

        fluxString.subscribe(n -> log.info("Number {}", n));
        log.info("------------------------------------------");
        StepVerifier.create(fluxString)
                .expectNext(1, 2, 3, 4, 5)
                .verifyComplete();

    }

    @Test
    public void fluxSubscriberFromList() {
        Flux<Integer> fluxString = Flux.fromIterable(List.of(1, 2, 3, 4, 5)).log();

        fluxString.subscribe(n -> log.info("Number {}", n));
        log.info("------------------------------------------");
        StepVerifier.create(fluxString)
                .expectNext(1, 2, 3, 4, 5)
                .verifyComplete();

    }

    @Test
    public void fluxSubscriberNumbersError() {

        Flux<Integer> fluxString = Flux.range(1, 5)
                .log()
                .map(i -> {
                    if (i == 4) {
                        throw new IndexOutOfBoundsException();
                    }
                    return i;
                });

        fluxString.subscribe(n -> log.info("Number {}", n),
                Throwable::printStackTrace,
                () -> log.info("DONE!"),
                subscription -> subscription.request(3));

        log.info("------------------------------------------");
        StepVerifier.create(fluxString)
                .expectNext(1, 2, 3)
                .expectError(IndexOutOfBoundsException.class)
                .verify();

    }

    @Test
    public void fluxSubscriberNumbersUglyBackpressure() {

        Flux<Integer> fluxString = Flux.range(1, 10)
                .log();


        fluxString.subscribe(new Subscriber<>() {

            private int count = 0;
            private Subscription subscription;
            private int requestCount = 2;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                this.subscription.request(2);
            }

            @Override
            public void onNext(Integer integer) {
                count++;
                if (count >= requestCount) {
                    count = 2;
                    subscription.request(requestCount);
                }
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        });

        log.info("------------------------------------------");
        StepVerifier.create(fluxString)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();

    }

    @Test
    public void fluxSubscriberNumbersNotSoUglyBackpressure() {

        Flux<Integer> fluxString = Flux.range(1, 10)
                .log();


        fluxString.subscribe(new BaseSubscriber<Integer>() {

            private int count = 0;
            private int requestCount = 2;

            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                request(requestCount);
            }

            @Override
            protected void hookOnNext(Integer value) {
                count++;
                if (count >= requestCount) {
                    count = 2;
                    request(requestCount);
                }
            }

        });

        log.info("------------------------------------------");
        StepVerifier.create(fluxString)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();

    }

    @Test
    public void fluxSubscriberPrettyBackpressure() {
        Flux<Integer> flux = Flux.range(1, 10)
                .log()
                .limitRate(3);

        flux.subscribe(n -> log.info("Number {}", n));
        log.info("------------------------------------------");
        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();

    }

    @Test
    public void fluxSubscriberIntervalOne() throws Exception {
        Flux<Long> interval = Flux.interval(Duration.ofMillis(100))
                .take(10)
                .log();

        interval.subscribe(i -> log.info("Number {}", i));

        Thread.sleep(3000);
    }

    @Test
    public void fluxSubscriberIntervalTwo() { ;
        StepVerifier.withVirtualTime(this::createInterval)
                .expectSubscription()
                .expectNoEvent(Duration.ofHours(24))
                .thenAwait(Duration.ofDays(2))
                .expectNext(0L)
                .thenAwait(Duration.ofDays(2))
                .expectNext(1L)
                .thenCancel()
                .verify();
    }

    private Flux<Long> createInterval() {
        return Flux.interval(Duration.ofDays(1))
                .log();
    }

    @Test
    public void connectableFlux() throws Exception {
        ConnectableFlux<Integer> connectableFlux = Flux.range(1, 10)
                //.log()
                .delayElements(Duration.ofMillis(100))
                .publish();

//        connectableFlux.connect();
//
//        log.info("Thread sleeping for 300ms");
//
//        Thread.sleep(300);
//
//        connectableFlux.subscribe(i -> log.info("Sub1 number {}", i));
//
//        log.info("Thread sleeping for 200ms");
//
//        Thread.sleep(200);
//
//        connectableFlux.subscribe(i -> log.info("Sub2 number {}", i));

        StepVerifier
                .create(connectableFlux)
                .then(connectableFlux::connect)
                .thenConsumeWhile(i -> i <= 5)
                .expectNext(6, 7, 8, 9, 10)
                .expectComplete()
                .verify();

    }

    @Test
    public void connectableFluxAutoConnect() throws Exception {
        Flux<Integer> fluxAutoConnect = Flux.range(1, 5)
                .log()
                .delayElements(Duration.ofMillis(100))
                .publish()
                .autoConnect(2);


        StepVerifier
                .create(fluxAutoConnect)
                .then(fluxAutoConnect::subscribe)
                .expectNext(1, 2, 3, 4, 5)
                .expectComplete()
                .verify();

    }


}
