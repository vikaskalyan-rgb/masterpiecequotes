package com.masterpiece.quotes.service;

import com.masterpiece.quotes.dto.DefaultsDto;
import com.masterpiece.quotes.dto.MaterialSpecItemDto;
import com.masterpiece.quotes.dto.TermItemDto;
import com.masterpiece.quotes.entity.DefaultMaterialSpecItem;
import com.masterpiece.quotes.entity.DefaultTermItem;
import com.masterpiece.quotes.repository.DefaultMaterialSpecItemRepository;
import com.masterpiece.quotes.repository.DefaultTermItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultsService {

    private final DefaultMaterialSpecItemRepository specRepository;
    private final DefaultTermItemRepository termRepository;

    public DefaultsService(DefaultMaterialSpecItemRepository specRepository,
                            DefaultTermItemRepository termRepository) {
        this.specRepository = specRepository;
        this.termRepository = termRepository;
    }

    @Transactional(readOnly = true)
    public DefaultsDto get() {
        DefaultsDto dto = new DefaultsDto();

        List<MaterialSpecItemDto> specs = new ArrayList<>();
        for (DefaultMaterialSpecItem s : specRepository.findAllByOrderBySortOrderAsc()) {
            MaterialSpecItemDto d = new MaterialSpecItemDto();
            d.setId(s.getId());
            d.setItemLabel(s.getItemLabel());
            d.setDetail(s.getDetail());
            d.setBrand(s.getBrand());
            d.setSortOrder(s.getSortOrder());
            specs.add(d);
        }
        dto.setMaterialSpecItems(specs);

        List<TermItemDto> terms = new ArrayList<>();
        for (DefaultTermItem t : termRepository.findAllByOrderBySortOrderAsc()) {
            TermItemDto d = new TermItemDto();
            d.setId(t.getId());
            d.setText(t.getText());
            d.setSortOrder(t.getSortOrder());
            terms.add(d);
        }
        dto.setTermItems(terms);

        return dto;
    }

    @Transactional
    public DefaultsDto update(DefaultsDto request) {
        specRepository.deleteAll();
        int specOrder = 0;
        for (MaterialSpecItemDto d : request.getMaterialSpecItems()) {
            DefaultMaterialSpecItem s = new DefaultMaterialSpecItem();
            s.setItemLabel(d.getItemLabel());
            s.setDetail(d.getDetail());
            s.setBrand(d.getBrand());
            s.setSortOrder(d.getSortOrder() != null ? d.getSortOrder() : specOrder++);
            specRepository.save(s);
        }

        termRepository.deleteAll();
        int termOrder = 0;
        for (TermItemDto d : request.getTermItems()) {
            DefaultTermItem t = new DefaultTermItem();
            t.setText(d.getText());
            t.setSortOrder(d.getSortOrder() != null ? d.getSortOrder() : termOrder++);
            termRepository.save(t);
        }

        return get();
    }
}
