package inkatech.detective.api;

import inkatech.detective.config.CatalogoVariables;
import inkatech.detective.config.VariableConfig;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/variables")
public final class VariableController {

    private final CatalogoVariables catalogo;

    public VariableController(CatalogoVariables catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping
    public List<VariableConfig> listar() {
        return catalogo.listar();
    }
}
