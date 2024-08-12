package com.depository_manage.controller.clck;
import com.depository_manage.entity.clck.Tax;
import com.depository_manage.service.clck.TaxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/taxs")
public class TaxController {
    private final TaxService taxService;
    public TaxController(TaxService taxService) {this.taxService = taxService;}

    @GetMapping("/")
    public ResponseEntity<List<Tax>> getAll(@RequestParam Map<String, Object> params) {
        return ResponseEntity.ok(taxService.findAll(params));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tax> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(taxService.findById(id));
    }

    @PostMapping("/")
    public ResponseEntity<Void> create(@RequestBody Tax tax) {
        taxService.insert(tax);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @RequestBody Tax tax) {
        tax.setId(id);
        taxService.update(tax);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        taxService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
