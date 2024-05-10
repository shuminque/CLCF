package com.depository_manage.controller.clck;
import com.depository_manage.entity.clck.Purchaser;
import com.depository_manage.service.clck.PurchaserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/purchasers")
public class PurchaserController {
    private final PurchaserService purchaserService;
    public PurchaserController(PurchaserService purchaserService) {this.purchaserService = purchaserService;}

    @GetMapping("/")
    public ResponseEntity<List<Purchaser>> getAll() {return ResponseEntity.ok(purchaserService.findAll());}

    @GetMapping("/{id}")
    public ResponseEntity<Purchaser> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(purchaserService.findById(id));
    }

    @PostMapping("/")
    public ResponseEntity<Void> create(@RequestBody Purchaser purchaser) {
        purchaserService.insert(purchaser);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @RequestBody Purchaser purchaser) {
        purchaser.setId(id);
        purchaserService.update(purchaser);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        purchaserService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
