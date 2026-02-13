package ekstraklasa.predictor.controllers;

import ekstraklasa.predictor.service.ConsumableFolderWatcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author Witold Drożdżowski (drozdzowski.witold@gmail.com)
 * @since 02.2026
 */
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class ConsumeFileTriggerController {

    private final ConsumableFolderWatcherService consumableFolderWatcherService;

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/trigger"
    )

    public ResponseEntity<Boolean> teamStrengthGet(
    ) {
        var result = consumableFolderWatcherService.consume();
        return ResponseEntity.ok(result);
    }
}
