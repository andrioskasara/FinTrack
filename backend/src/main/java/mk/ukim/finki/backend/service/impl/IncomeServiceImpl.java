package mk.ukim.finki.backend.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.backend.mapper.IncomeMapper;
import mk.ukim.finki.backend.model.dto.transaction.IncomeDto;
import mk.ukim.finki.backend.model.dto.transaction.IncomeRequest;
import mk.ukim.finki.backend.model.entity.Category;
import mk.ukim.finki.backend.model.entity.Income;
import mk.ukim.finki.backend.model.entity.User;
import mk.ukim.finki.backend.repository.CategoryRepository;
import mk.ukim.finki.backend.repository.IncomeRepository;
import mk.ukim.finki.backend.repository.UserRepository;
import mk.ukim.finki.backend.service.IncomeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static mk.ukim.finki.backend.util.TransactionServiceMessages.INCOME_NOT_FOUND;

/**
 * Income service implementation extending base financial transaction logic.
 */
@Slf4j
@Service
public class IncomeServiceImpl extends AbstractTransactionService<Income> implements IncomeService {

    private final IncomeRepository incomeRepository;
    private final IncomeMapper incomeMapper;

    public IncomeServiceImpl(IncomeRepository incomeRepository,
                             UserRepository userRepository,
                             CategoryRepository categoryRepository,
                             IncomeMapper incomeMapper) {
        super(userRepository, categoryRepository);
        this.incomeRepository = incomeRepository;
        this.incomeMapper = incomeMapper;
    }

    @Override
    public List<IncomeDto> getAll() {
        User user = getCurrentUser();
        List<Income> incomes = incomeRepository.findAllByUser_IdOrderByDateDescCreatedAtDesc(user.getId());

        return incomes.stream()
                .map(incomeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public IncomeDto getById(UUID id) {
        User user = getCurrentUser();
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(INCOME_NOT_FOUND));
        validateOwnership(income, user);
        return incomeMapper.toDto(income);
    }

    @Override
    @Transactional
    public IncomeDto create(IncomeRequest request) {
        User user = getCurrentUser();

        Category category = findCategoryOrThrow(request.getCategoryId());
        validateCategoryOwnership(category, user);

        Income income = Income.builder()
                .user(user)
                .category(category)
                .amount(request.getAmount())
                .date(request.getDate())
                .description(request.getDescription())
                .build();

        incomeRepository.save(income);

        log.info("User [{}] created an income: amount={}, category={}, date={}",
                user.getEmail(), income.getAmount(), category.getName(), income.getDate());

        return incomeMapper.toDto(income);
    }

    @Override
    @Transactional
    public IncomeDto update(UUID id, IncomeRequest request) {
        User user = getCurrentUser();
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(INCOME_NOT_FOUND));
        validateOwnership(income, user);

        Category category = findCategoryOrThrow(request.getCategoryId());
        validateCategoryOwnership(category, user);

        income.setAmount(request.getAmount());
        income.setCategory(category);
        income.setDate(request.getDate());
        income.setDescription(request.getDescription());

        incomeRepository.save(income);

        log.info("User [{}] updated income ID [{}]: amount={}, category={}, date={}",
                user.getEmail(), id, income.getAmount(), category.getName(), income.getDate());

        return incomeMapper.toDto(income);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        User user = getCurrentUser();
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(INCOME_NOT_FOUND));
        validateOwnership(income, user);

        incomeRepository.delete(income);

        log.info("User [{}] deleted income ID [{}], amount={}, category={}, date={}",
                user.getEmail(), id, income.getAmount(), income.getCategory().getName(), income.getDate());
    }
}
