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

@Service
public class DefaultsService {

    private final DefaultMaterialSpecItemRepository specRepository;
    private final DefaultTermItemRepository termRepository;

    public DefaultsService(DefaultMaterialSpecItemRepository specRepository, DefaultTermItemRepository termRepository) {
        this.specRepository = specRepository;
        this.termRepository = termRepository;
    }

    @Transactional(readOnly = true)
    public DefaultsDto get() {
        return new DefaultsDto(
                specRepository.findAllByOrderBySortOrderAsc().stream()
                        .map(s -> new MaterialSpecItemDto(s.getItemLabel(), s.getDetail(), s.getBrand()))
                        .toList(),
                termRepository.findAllByOrderBySortOrderAsc().stream()
                        .map(t -> new TermItemDto(t.getText()))
                        .toList());
    }

    @Transactional
    public DefaultsDto update(DefaultsDto request) {
        specRepository.deleteAllInBatch();
        int specOrder = 0;
        for (MaterialSpecItemDto d : request.specsOrEmpty()) {
            DefaultMaterialSpecItem s = new DefaultMaterialSpecItem();
            s.setItemLabel(d.itemLabel().trim());
            s.setDetail(blankToNull(d.detail()));
            s.setBrand(blankToNull(d.brand()));
            s.setSortOrder(specOrder++);
            specRepository.save(s);
        }

        termRepository.deleteAllInBatch();
        int termOrder = 0;
        for (TermItemDto d : request.termsOrEmpty()) {
            DefaultTermItem t = new DefaultTermItem();
            t.setText(d.text().trim());
            t.setSortOrder(termOrder++);
            termRepository.save(t);
        }
        return get();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
