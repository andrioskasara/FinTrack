package mk.ukim.finki.backend.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Expense entity extending common fields from TransactionBase.
 */
@Entity
@Table(name = "expenses")
@NoArgsConstructor
@SuperBuilder
public class Expense extends TransactionBase {
}
