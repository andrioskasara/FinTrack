package mk.ukim.finki.backend.service;

import jakarta.persistence.EntityNotFoundException;
import mk.ukim.finki.backend.exception.UnauthorizedTransactionAccessException;

import java.util.List;
import java.util.UUID;

/**
 * Generic service interface defining CRUD operations for financial transaction entities.
 *
 * @param <Dto>     type representing the output Data Transfer Object
 * @param <Request> type used to capture input data needed to create or update the entity
 */
public interface TransactionService<Dto, Request> {

    /**
     * Retrieves all entities belonging to the current authenticated user.
     *
     * @return list of all entities' DTOs
     */
    List<Dto> getAll();

    /**
     * Retrieves a single entity by its unique identifier if owned by the current user.
     *
     * @param id identifier of the entity
     * @return DTO representation of the entity
     * @throws EntityNotFoundException                if entity does not exist
     * @throws UnauthorizedTransactionAccessException if the entity is not owned by the user
     */
    Dto getById(UUID id);

    /**
     * Creates a new entity for the current user.
     *
     * @param request DTO containing data for creation
     * @return DTO of created entity
     * @throws EntityNotFoundException if referenced data, such as category, is invalid
     */
    Dto create(Request request);

    /**
     * Updates an existing entity owned by the current user.
     *
     * @param id      identifier of the entity to update
     * @param request DTO containing updated data
     * @return DTO of the updated entity
     * @throws EntityNotFoundException                if entity does not exist
     * @throws UnauthorizedTransactionAccessException if the entity is not owned by the user
     */
    Dto update(UUID id, Request request);

    /**
     * Deletes an entity owned by the current user.
     *
     * @param id identifier of the entity to delete
     * @throws EntityNotFoundException                if entity does not exist
     * @throws UnauthorizedTransactionAccessException if the entity is not owned by the user
     */
    void delete(UUID id);
}
