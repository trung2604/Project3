package com.project3.menuservice.query.projection;

import com.project3.menuservice.command.entity.Combo;
import com.project3.menuservice.command.entity.ComboRepository;
import com.project3.menuservice.query.dto.ComboResponse;
import com.project3.menuservice.query.queries.GetAllCombosQuery;
import com.project3.menuservice.query.queries.GetComboByIdQuery;
import com.project3.menuservice.query.queries.GetCombosByMenuItemQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ComboProjection {

    @Autowired
    private ComboRepository comboRepository;

    @QueryHandler
    public List<ComboResponse> getAll(GetAllCombosQuery query) {
        List<Combo> combos = comboRepository.findAll();
        return combos.stream()
                .map(ComboResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @QueryHandler
    public ComboResponse getById(GetComboByIdQuery query) {
        Combo combo = comboRepository.findById(query.getComboId())
                .orElseThrow(() -> new RuntimeException("Combo not found"));
        return ComboResponse.fromEntity(combo);
    }

    @QueryHandler
    public List<ComboResponse> getByMenuItem(GetCombosByMenuItemQuery query) {
        List<Combo> combos = comboRepository.findByMenuItemIdAndActive(query.getMenuItemId());
        return combos.stream()
                .map(ComboResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
