import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

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

}
