package com.depository_manage.controller.clck;
import com.depository_manage.entity.clck.Area;
import com.depository_manage.service.clck.AreaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/areas")
public class AreaController {
    private final AreaService areaService;
    public AreaController(AreaService areaService) {this.areaService = areaService;}

    @GetMapping("/")
    public ResponseEntity<List<Area>> getAll(@RequestParam Map<String, Object> params) {
        return ResponseEntity.ok(areaService.findAll(params));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Area> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(areaService.findById(id));
    }

    @PostMapping("/")
    public ResponseEntity<Void> create(@RequestBody Area area) {
        areaService.insert(area);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @RequestBody Area area) {
        area.setId(id);
        areaService.update(area);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        areaService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
