package com.missio.fluencia_leitora.cadastros.dominio;

import com.missio.fluencia_leitora.cadastros.dominio.dto.CicloResponse;
import com.missio.fluencia_leitora.cadastros.dominio.dto.TipoLeituraResponse;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Domínios fixos, somente leitura (seed via Flyway). Nenhum outro verbo é
 * mapeado nesses paths, então POST/PUT/DELETE retornam 405 pelo
 * comportamento padrão do Spring.
 */
@RestController
@RequestMapping("/api/v1")
public class DominioFixoController {

    private final CicloRepository cicloRepository;
    private final TipoLeituraRepository tipoLeituraRepository;

    public DominioFixoController(CicloRepository cicloRepository, TipoLeituraRepository tipoLeituraRepository) {
        this.cicloRepository = cicloRepository;
        this.tipoLeituraRepository = tipoLeituraRepository;
    }

    @GetMapping("/ciclos")
    public List<CicloResponse> listarCiclos() {
        return cicloRepository.findAll(Sort.by("id")).stream()
                .map(CicloResponse::from)
                .toList();
    }

    @GetMapping("/tipos-leitura")
    public List<TipoLeituraResponse> listarTiposLeitura() {
        return tipoLeituraRepository.findAll(Sort.by("id")).stream()
                .map(TipoLeituraResponse::from)
                .toList();
    }
}
