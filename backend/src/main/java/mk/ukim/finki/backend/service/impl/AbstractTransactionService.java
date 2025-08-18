package mk.ukim.finki.backend.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.exception.UnauthorizedTransactionAccessException;
import mk.ukim.finki.backend.model.entity.Category;
import mk.ukim.finki.backend.model.entity.TransactionBase;
import mk.ukim.finki.backend.model.entity.User;
import mk.ukim.finki.backend.repository.CategoryRepository;
import mk.ukim.finki.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static mk.ukim.finki.backend.util.TransactionServiceMessages.*;

/**
 * Abstract base service to encapsulate common financial transaction logic for Expense and Income.
 *
 * @param <T> TransactionBase subtype (Expense or Income)
 */
@RequiredArgsConstructor
public abstract class AbstractTransactionService<T extends TransactionBase> {

    protected final UserRepository userRepository;
    protected final CategoryRepository categoryRepository;

    protected User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));
    }

    protected void validateCategoryOwnership(Category category, User user) {
        if (category.isPredefined()) return;
        if (category.getUser() == null || !category.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedTransactionAccessException(CATEGORY_UNAUTHORIZED_ACCESS);
        }
    }

    protected void validateOwnership(T entity, User user) {
        if (!entity.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedTransactionAccessException(UNAUTHORIZED_ACCESS);
        }
    }

    protected Category findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CATEGORY_NOT_FOUND));
    }
}