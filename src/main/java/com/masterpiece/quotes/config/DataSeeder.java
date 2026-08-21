package com.masterpiece.quotes.config;

import com.masterpiece.quotes.entity.DefaultMaterialSpecItem;
import com.masterpiece.quotes.entity.DefaultTermItem;
import com.masterpiece.quotes.repository.DefaultMaterialSpecItemRepository;
import com.masterpiece.quotes.repository.DefaultTermItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final DefaultMaterialSpecItemRepository specRepository;
    private final DefaultTermItemRepository termRepository;

    public DataSeeder(DefaultMaterialSpecItemRepository specRepository,
                       DefaultTermItemRepository termRepository) {
        this.specRepository = specRepository;
        this.termRepository = termRepository;
    }

    @Override
    public void run(String... args) {
        if (specRepository.count() == 0) {
            seedSpec("Plywood - Kitchen Base", "BWP 710 Grade, Moisture Resistant", "GAMA Gurjan Hardwood Plywood", 0);
            seedSpec("Plywood - All Other Work", "Moisture Resistant", "GAMA Hardwood Plywood", 1);
            seedSpec("Accessories", "Zinc Fittings", "Sleek / Ebco", 2);
            seedSpec("Outer Laminates", "Colour, 1mm", "Advance Brand", 3);
            seedSpec("Inside Laminates", "Fabric Liner, 0.8mm", "Advance Lam", 4);
            seedSpec("Joineries", "Hinges / Locks", "Ebco / Sleek / Europa", 5);
        }

        if (termRepository.count() == 0) {
            seedTerm("This quote is exclusive of GST. 18% extra if bills are required.", 0);
            seedTerm("50% payment along with confirmed work order.", 1);
            seedTerm("45% on completion of box fixing (carcass).", 2);
            seedTerm("5% on completion of work.", 3);
            seedTerm("Delivery within 21 days of order acceptance. Duration: 21 working days from order date.", 4);
            seedTerm("Any additional work beyond this quote will be charged separately.", 5);
        }
    }

    private void seedSpec(String label, String detail, String brand, int order) {
        DefaultMaterialSpecItem s = new DefaultMaterialSpecItem();
        s.setItemLabel(label);
        s.setDetail(detail);
        s.setBrand(brand);
        s.setSortOrder(order);
        specRepository.save(s);
    }

    private void seedTerm(String text, int order) {
        DefaultTermItem t = new DefaultTermItem();
        t.setText(text);
        t.setSortOrder(order);
        termRepository.save(t);
    }
}
