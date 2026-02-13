package ekstraklasa.predictor.controllers;

import ekstraklasa.predictor.api.TablePredictionApi;
import ekstraklasa.predictor.cache.SimulatedStandingCacheRepository;
import ekstraklasa.predictor.model.TablePredictionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api") // add /api prefix to match expected URL
public class TablePredictorController implements TablePredictionApi {

    private final SimulatedStandingCacheRepository simulatedStandingCacheRepository;

    public TablePredictorController(SimulatedStandingCacheRepository simulatedStandingCacheRepository) {
        this.simulatedStandingCacheRepository = simulatedStandingCacheRepository;
    }

    @Override
    @GetMapping(value = "/table-prediction", produces = "application/json")
    public ResponseEntity<List<TablePredictionModel>> tablePredictionGet() {
        return ResponseEntity.ok(simulatedStandingCacheRepository.getAll());
    }
}