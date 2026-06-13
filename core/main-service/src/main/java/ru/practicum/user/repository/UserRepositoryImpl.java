package ru.practicum.user.repository;

import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import ru.practicum.user.model.QUser;
import ru.practicum.user.model.User;

import java.util.List;

@Component
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @PersistenceContext
    private EntityManager entityManager;


    public UserRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public Page<User> findByIdInWithPagination(List<Long> ids, Pageable pageable) {
        // Проверка на null и пустой список ID — если нет ID, возвращаем пустую страницу
        if (ids == null || ids.isEmpty()) {
            return Page.empty(pageable);
        }

        // Получаем статический Q‑класс для сущности User — нужен для типобезопасных запросов
        QUser user = QUser.user;

        // Создаём запрос: выбираем все поля из таблицы User, где ID есть в списке ids
        var query = queryFactory.selectFrom(user)
                .where(user.id.in(ids));

        // Выполняем подсчёт общего количества записей (для пагинации)
        long total = query.fetchCount();

        // Выполняем запрос с пагинацией:
        // - offset — смещение (сколько записей пропустить)
        // - limit — сколько записей вернуть
        // - fetch() — выполняем запрос и получаем результат
        List<User> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // Создаём объект Page с результатами:
        // - content — список пользователей на текущей странице
        // - pageable — параметры пагинации (номер страницы, размер и т.д.)
        // - total — общее количество записей (нужно для расчёта количества страниц)
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public long deleteByIdCustom(Long id) {
        // Получаем Q‑класс для построения типобезопасного запроса на удаление
        QUser user = QUser.user;

        // Создаём запрос на удаление: удаляем запись из таблицы User,
        // где ID равен переданному параметру
        JPADeleteClause deleteClause = new JPADeleteClause(entityManager, user)
                .where(user.id.eq(id));

        // Выполняем удаление и возвращаем количество удалённых записей
        // Если запись не найдена — вернёт 0
        return deleteClause.execute();
    }
}
