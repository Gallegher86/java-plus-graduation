package ru.practicum.controller;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.dto.ScoredEvent;
import ru.practicum.ewm.stats.proto.analyzer.*;
import ru.practicum.service.AnalyzerService;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcController
        extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final AnalyzerService analyzerService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {

        log.debug("GetRecommendationsForUser userId={}, maxResults={}",
                request.getUserId(), request.getMaxResults());

        try {
            List<ScoredEvent> recommendations =
                    analyzerService.getRecommendationsForUser(
                            request.getUserId(),
                            request.getMaxResults()
                    );

            for (ScoredEvent r : recommendations) {
                RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                        .setEventId(r.eventId())
                        .setScore(r.score())
                        .build();

                responseObserver.onNext(proto);
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in getRecommendationsForUser userId={}"
                    , request.getUserId(), e);
            responseObserver
                    .onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {

        log.debug("GetSimilarEvents userId={}, eventId={}, maxResults={}",
                request.getUserId(),
                request.getEventId(),
                request.getMaxResults());

        try {
            List<ScoredEvent> similarEvents =
                    analyzerService.getSimilarEvents(
                            request.getUserId(),
                            request.getEventId(),
                            request.getMaxResults()
                    );

            for (ScoredEvent se : similarEvents) {
                RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                        .setEventId(se.eventId())
                        .setScore(se.score())
                        .build();

                responseObserver.onNext(proto);
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in getSimilarEvents userId={}, eventId={}"
                    , request.getUserId(), request.getEventId(), e);
            responseObserver
                    .onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {

        log.debug("GetInteractionsCount eventIds={}", request.getEventIdList());

        try {
            List<ScoredEvent> counts =
                    analyzerService.getInteractionsCount(request.getEventIdList());

            for (ScoredEvent c : counts) {
                RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                        .setEventId(c.eventId())
                        .setScore(c.score())
                        .build();

                responseObserver.onNext(proto);
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in getInteractionsCount eventIds={}",
                    request.getEventIdList(), e);
            responseObserver
                    .onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException()
            );
        }
    }
}
