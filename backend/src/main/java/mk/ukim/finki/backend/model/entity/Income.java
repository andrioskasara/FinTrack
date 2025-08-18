package mk.ukim.finki.backend.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Income entity extending common fields from TransactionBase.
 */
@Entity
@Table(name = "incomes")
@NoArgsConstructor
@SuperBuilder
public class Income extends TransactionBase {
}
