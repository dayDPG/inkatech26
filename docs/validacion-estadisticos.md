# Validación externa de estadísticos
Se contrastaron fixtures pequeños del código Java con cálculos independientes en Python 3.10.4, NumPy 2.2.6 y SciPy 1.15.3. Los mismos casos están cubiertos por `TheilSenTest`, `MannKendallTest` y `CorrelacionTest`.

## Resultados reproducibles

| Método | Fixture | Java | Referencia Python |
|---|---|---|---|
| Theil–Sen, IC 95 % | 18 pares con huecos en `x` | pendiente `0.5470588235294117`; IC `[0.5272727272727273, 0.5666666666666668]` | `scipy.stats.theilslopes(y, x, alpha=.95, method="separate")`: mismos valores |
| Mann–Kendall con empates | `y = [2,3,3,4,4,4,5,6,6,7,8,8]` | `S=60`, varianza `206`, `Z=4.110724784320544`, `p=3.944190958715079e-5` | Conteo de pares, corrección por empates, continuidad y `2*scipy.stats.norm.sf(abs(Z))`: mismos valores |
| Spearman con rezago | A mensual `0..71`; B contiene `-1` en enero de 2018 y luego `0..71`, desplazados un mes; anomalías por mes y pares alineados a rezago 1 | `n=72`, `rho=0.9967421799986831`, `p≈2.772907831708384e-78` | `scipy.stats.spearmanr` sobre las anomalías alineadas: `rho=0.9967421799986833`, `p=2.772907831708384e-78` |

El p de Mann–Kendall aplica corrección de continuidad. Por esa diferencia de método, no debe compararse directamente con el p predeterminado de `scipy.stats.kendalltau`, que puede usar otra aproximación asintótica.

Para Spearman, el backend evalúa la misma cola bilateral de la distribución t mediante la beta incompleta regularizada. Esto evita perder precisión por restar una CDF redondeada a `1` cuando el p es muy pequeño.

## Código de referencia

El caso de Theil–Sen se reproduce directamente con SciPy:

```python
from scipy.stats import theilslopes

x = [0, 1, 2, 4, 5, 7, 8, 9, 11, 12, 14, 15, 17, 18, 19, 21, 22, 23]
y = [1.0, 1.4, 1.9, 3.1, 3.4, 4.6, 5.2, 5.5, 6.9, 7.1,
     8.6, 8.9, 10.4, 10.1, 11.2, 12.9, 12.7, 13.8]
print(theilslopes(y, x, alpha=0.95, method="separate"))
```

Para Spearman, la referencia arma las climatologías mensuales antes de alinear:

```python
import numpy as np
from scipy.stats import spearmanr

a = np.arange(72, dtype=float)
b = np.r_[-1.0, a]
mes_a = np.arange(72) % 12
mes_b = np.arange(73) % 12
clima_a = np.array([a[mes_a == m].mean() for m in range(12)])
clima_b = np.array([b[mes_b == m].mean() for m in range(12)])
print(spearmanr(a - clima_a[mes_a], (b - clima_b[mes_b])[1:]))
```

Para Mann–Kendall, este cálculo Python aplica explícitamente la corrección por empates y la corrección de continuidad usada por el proyecto:

```python
import numpy as np
from scipy.stats import norm

y = np.array([2, 3, 3, 4, 4, 4, 5, 6, 6, 7, 8, 8], dtype=float)
n = len(y)
s = sum(np.sign(y[j] - y[i]) for i in range(n - 1) for j in range(i + 1, n))
_, counts = np.unique(y, return_counts=True)
empates = sum(c * (c - 1) * (2 * c + 5) for c in counts if c > 1)
varianza = (n * (n - 1) * (2 * n + 5) - empates) / 18
z = (s - np.sign(s)) / np.sqrt(varianza)
p = 2 * norm.sf(abs(z))
print(s, varianza, z, p)
```

## Faltantes y alcance

Las pruebas también verifican que `NaN`, días ausentes y puntos parciales no se conviertan en ceros ni entren a los pares de correlación o a las tendencias. La agregación de lluvia conserva el total de días válidos, pero marca el mes como parcial. Estas pruebas validan las fórmulas y reglas de borde con series controladas; no son una validación independiente de la calidad física de los datos satelitales.
