package com.example.weatherapp.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.weatherapp.R;
import com.example.weatherapp.controller.WeatherController;
import com.example.weatherapp.model.WeatherData;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.Locale;

/**
 * MainActivity
 * ------------
 * É a "V" (View) do padrão MVC. Sua ÚNICA responsabilidade é a interface:
 * mostrar campos, botões, receber toques do usuário e exibir os dados. Ela NÃO
 * faz chamadas de rede diretamente — delega tudo ao {@link WeatherController}.
 *
 * Implementa {@link WeatherController.WeatherView}, ou seja, o "contrato" que o
 * Controller usa para devolver os resultados (carregando / sucesso / erro).
 *
 * Funcionalidades:
 *  - Geolocalização automática com FusedLocationProviderClient (botão GPS);
 *  - Pedido de permissão de localização em tempo de execução (runtime);
 *  - Entrada manual de latitude/longitude com botão "Buscar";
 *  - Exibição de cidade, temperatura aparente, descrição, umidade e pressão.
 *
 * @author Projeto didático WeatherApp
 */
public class MainActivity extends AppCompatActivity implements WeatherController.WeatherView {

    // ------------------------------------------------------------------
    // Referências de tela (Views) e colaboradores
    // ------------------------------------------------------------------

    private WeatherController controller;             // Controller do MVC
    private FusedLocationProviderClient locationClient; // Cliente de localização (GPS)

    // Widgets do layout activity_main.xml
    private ProgressBar progressBar;
    private TextView tvCity;
    private TextView tvTemperature;
    private TextView tvDescription;
    private TextView tvHumidity;
    private TextView tvPressure;
    private TextView tvError;
    private View contentGroup;         // Bloco que agrupa os dados do clima
    private EditText etLatitude;
    private EditText etLongitude;
    private Button btnSearch;
    private Button btnMyLocation;

    /**
     * Launcher moderno para solicitar a permissão de localização em runtime.
     * Substitui o antigo onRequestPermissionsResult. Ao receber a resposta do
     * usuário, decidimos se buscamos a localização ou avisamos que foi negada.
     */
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        Boolean fine = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                        Boolean coarse = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                        // Se qualquer uma das permissões foi concedida, busca a localização.
                        if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                            fetchDeviceLocation();
                        } else {
                            Toast.makeText(this,
                                    R.string.permissao_negada,
                                    Toast.LENGTH_LONG).show();
                        }
                    });

    // ------------------------------------------------------------------
    // Ciclo de vida da Activity
    // ------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Infla o layout XML.

        // 1) Liga as referências Java aos elementos do XML (findViewById).
        bindViews();

        // 2) Cria o Controller (passando o contexto desta Activity).
        controller = new WeatherController(this);

        // 3) Cria o cliente de localização (GPS/rede) do Google Play Services.
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        // 4) Configura os cliques dos botões.
        setupListeners();
    }

    /**
     * Localiza cada widget do layout e guarda a referência nos atributos.
     */
    private void bindViews() {
        progressBar    = findViewById(R.id.progressBar);
        tvCity         = findViewById(R.id.tvCity);
        tvTemperature  = findViewById(R.id.tvTemperature);
        tvDescription  = findViewById(R.id.tvDescription);
        tvHumidity     = findViewById(R.id.tvHumidity);
        tvPressure     = findViewById(R.id.tvPressure);
        tvError        = findViewById(R.id.tvError);
        contentGroup   = findViewById(R.id.contentGroup);
        etLatitude     = findViewById(R.id.etLatitude);
        etLongitude    = findViewById(R.id.etLongitude);
        btnSearch      = findViewById(R.id.btnSearch);
        btnMyLocation  = findViewById(R.id.btnMyLocation);
    }

    /**
     * Define o que acontece ao tocar em cada botão.
     */
    private void setupListeners() {
        // Botão "Buscar": usa as coordenadas digitadas manualmente.
        btnSearch.setOnClickListener(v -> onManualSearch());

        // Botão "Usar minha localização": aciona o fluxo de permissão + GPS.
        btnMyLocation.setOnClickListener(v -> onUseMyLocation());
    }

    // ------------------------------------------------------------------
    // Ações do usuário
    // ------------------------------------------------------------------

    /**
     * Lê os campos de latitude/longitude, valida e pede o clima ao Controller.
     */
    private void onManualSearch() {
        String latText = etLatitude.getText().toString().trim();
        String lonText = etLongitude.getText().toString().trim();

        // Validação simples: campos não podem estar vazios.
        if (TextUtils.isEmpty(latText) || TextUtils.isEmpty(lonText)) {
            Toast.makeText(this, R.string.erro_coordenadas_vazias, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double lat = Double.parseDouble(latText);
            double lon = Double.parseDouble(lonText);
            // Delega ao Controller — a View não sabe "como" o dado é buscado.
            controller.loadWeather(lat, lon, this);
        } catch (NumberFormatException e) {
            // O usuário digitou algo que não é número.
            Toast.makeText(this, R.string.erro_coordenadas_invalidas, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Verifica se já temos permissão de localização; se sim, busca a posição,
     * senão, solicita a permissão ao usuário.
     */
    private void onUseMyLocation() {
        boolean temPermissao = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (temPermissao) {
            fetchDeviceLocation();
        } else {
            // Dispara o diálogo de permissão; o resultado chega no launcher acima.
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    /**
     * Obtém a última localização conhecida do dispositivo e, com ela, pede o
     * clima ao Controller. É anotado com @SuppressWarnings pois a permissão já
     * foi verificada antes de chamar este método.
     */
    @SuppressWarnings("MissingPermission")
    private void fetchDeviceLocation() {
        showLoading(); // Feedback imediato ao usuário.

        locationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        // Preenche os campos manuais para o usuário ver a coordenada usada.
                        etLatitude.setText(String.valueOf(lat));
                        etLongitude.setText(String.valueOf(lon));
                        controller.loadWeather(lat, lon, this);
                    } else {
                        // getLastLocation pode ser nulo se o GPS nunca foi usado.
                        showError(getString(R.string.erro_localizacao_indisponivel));
                    }
                })
                .addOnFailureListener(this, e ->
                        showError(getString(R.string.erro_localizacao_falhou)));
    }

    // ------------------------------------------------------------------
    // Implementação do contrato WeatherController.WeatherView
    // ------------------------------------------------------------------

    /**
     * Mostra o indicador de carregamento e esconde dados/erros anteriores.
     */
    @Override
    public void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        contentGroup.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }

    /**
     * Recebe o WeatherData pronto e preenche os campos da tela.
     *
     * @param data objeto com os dados de clima
     */
    @Override
    public void showWeather(@NonNull WeatherData data) {
        progressBar.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
        contentGroup.setVisibility(View.VISIBLE);

        // Locale.getDefault() formata os números conforme o idioma do aparelho.
        tvCity.setText(data.getCityName());
        tvTemperature.setText(String.format(Locale.getDefault(), "%.1f°C",
                data.getApparentTemperature()));
        tvDescription.setText(data.getWeatherDescription());
        tvHumidity.setText(getString(R.string.rotulo_umidade,
                data.getRelativeHumidity2m()));
        tvPressure.setText(getString(R.string.rotulo_pressao,
                data.getSurfacePressure()));
    }

    /**
     * Exibe uma mensagem de erro amigável (TextView + Toast).
     *
     * @param message texto do erro em português
     */
    @Override
    public void showError(String message) {
        progressBar.setVisibility(View.GONE);
        contentGroup.setVisibility(View.GONE);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
