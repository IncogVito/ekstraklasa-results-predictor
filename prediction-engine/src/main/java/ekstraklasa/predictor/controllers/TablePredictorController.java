package ekstraklasa.predictor.controllers;

import ekstraklasa.predictor.api.TablePredictionApi;
import ekstraklasa.predictor.model.LeagueTableModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api") // add /api prefix to match expected URL
public class TablePredictorController implements TablePredictionApi {

    @Override
    @GetMapping(value = "/table-prediction", produces = "application/json")
    public ResponseEntity<LeagueTableModel> tablePredictionGet(
            Integer simulationIterations) {

        return ResponseEntity.ok(new LeagueTableModel());
    }
}