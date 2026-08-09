package ru.practicum.controller;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.ewm.stats.proto.collector.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.collector.UserActionProto;
import ru.practicum.kafka.UserActionProducer;

import java.time.Instant;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionGrpcController extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final UserActionProducer producer;

    @Override
    public void collectUserAction(UserActionProto request,
                                  StreamObserver<Empty> responseObserver) {

        try {
            UserActionAvro action = UserActionAvro.newBuilder()
                    .setUserId(request.getUserId())
                    .setEventId(request.getEventId())
                    .setActionType(toAvro(request.getActionType()))
                    .setTimestamp(toInstant(request.getTimestamp()))
                    .build();

            producer.send(action);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception ex) {
            log.error(
                    "Ошибка обработки UserAction: userId={}, eventId={}",
                    request.getUserId(),
                    request.getEventId(),
                    ex
            );

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Не удалось обработать действие пользователя")
                            .withCause(ex)
                            .asRuntimeException()
            );
        }
    }

    private ActionTypeAvro toAvro(ActionTypeProto proto) {
        return switch (proto) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED ->
                    throw new IllegalArgumentException("Unknown action type: " + proto);
        };
    }

    private Instant toInstant(Timestamp ts) {
        return Instant.ofEpochSecond(
                ts.getSeconds(),
                ts.getNanos()
        );
    }
}
