package com.example.weatherapp.model;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * WeatherRepository
 * -----------------
 * Camada de acesso a dados do Model (MVC). É a classe responsável por CONVERSAR
 * com a internet: faz as chamadas HTTP para a API Open-Meteo (clima) e para a
 * API BigDataCloud (nome da cidade por geocodificação reversa).
 *
 * Toda a parte de rede foi centralizada aqui — assim o Controller e a View não
 * precisam saber "como" os dados chegam, apenas "que" eles chegam. Isso é o
 * coração da separação de responsabilidades do MVC.
 *
 * Usa a biblioteca Retrofit 2 (chamadas HTTP declarativas) + Gson (conversão de
 * JSON para objetos Java).
 *
 * @author Projeto didático WeatherApp
 */
public class
weatherRepository {

    // ------------------------------------------------------------------
    // Constantes das URLs base das duas APIs utilizadas
    // ------------------------------------------------------------------

    /** URL base da API de previsão do tempo Open-Meteo. */
    private static final String OPEN_METEO_BASE_URL = "https://api.open-meteo.com/";

    /** URL base da API de geocodificação reversa BigDataCloud. */
    private static final String BIG_DATA_CLOUD_BASE_URL = "https://api.bigdatacloud.net/";

    /** Fuso horário usado na chamada, conforme o projeto de referência. */
    private static final String TIMEZONE = "America/Sao_Paulo";

    // ------------------------------------------------------------------
    // Interfaces de callback (comunicação assíncrona com o Controller)
    // ------------------------------------------------------------------

    /**
     * Interface de callback usada para devolver o resultado da busca de forma
     * assíncrona. Como a chamada de rede não é instantânea, não podemos
     * simplesmente "retornar" o valor; avisamos quem chamou quando o dado chega.
     */
    public interface WeatherCallback {
        /**
         * Chamado quando os dados de clima são obtidos com sucesso.
         *
         * @param data objeto WeatherData já preenchido
         */
        void onSuccess(WeatherData data);

        /**
         * Chamado quando ocorre qualquer erro (rede, servidor, JSON inválido).
         *
         * @param message mensagem amigável em português para exibir ao usuário
         */
        void onError(String message);
    }

    // ------------------------------------------------------------------
    // Definição das APIs para o Retrofit (interfaces anotadas)
    // ------------------------------------------------------------------

    /**
     * Interface que descreve, de forma declarativa, o endpoint de previsão do
     * tempo da Open-Meteo. O Retrofit lê estas anotações e gera o código de
     * rede automaticamente.
     */
    public interface OpenMeteoApi {
        /**
         * GET https://api.open-meteo.com/v1/forecast
         * Os parâmetros da query são preenchidos pelos argumentos anotados com @Query.
         *
         * @param latitude  latitude em graus decimais
         * @param longitude longitude em graus decimais
         * @param current   lista de variáveis atuais desejadas
         * @param timezone  fuso horário
         * @return uma chamada (Call) que devolve um WeatherResponse
         */
        @GET("v1/forecast")
        Call<WeatherResponse> getCurrentWeather(
                @Query("latitude") double latitude,
                @Query("longitude") double longitude,
                @Query("current") String current,
                @Query("timezone") String timezone
        );
    }

    /**
     * Interface que descreve o endpoint de geocodificação reversa do BigDataCloud,
     * usado para descobrir o nome da cidade a partir das coordenadas.
     */
    public interface GeocodingApi {
        /**
         * GET https://api.bigdatacloud.net/data/reverse-geocode-client
         *
         * @param latitude        latitude em graus decimais
         * @param longitude       longitude em graus decimais
         * @param localityLanguage idioma da resposta (ex.: "pt")
         * @return chamada que devolve um GeocodingResponse
         */
        @GET("data/reverse-geocode-client")
        Call<GeocodingResponse> reverseGeocode(
                @Query("latitude") double latitude,
                @Query("longitude") double longitude,
                @Query("localityLanguage") String localityLanguage
        );
    }

    // ------------------------------------------------------------------
    // Classes de resposta (mapeamento do JSON com Gson)
    // ------------------------------------------------------------------

    /**
     * Representa o JSON completo devolvido pela Open-Meteo. Só declaramos os
     * campos que nos interessam; o Gson ignora o restante.
     */
    public static class WeatherResponse {
        /** Objeto "current" do JSON, com os dados atuais do tempo. */
        @SerializedName("current")
        public Current current;

        /**
         * Classe interna que representa o objeto "current" do JSON.
         * Cada @SerializedName liga o nome do campo no JSON ao atributo Java.
         */
        public static class Current {
            @SerializedName("apparent_temperature")
            public double apparentTemperature;

            @SerializedName("relative_humidity_2m")
            public int relativeHumidity2m;

            @SerializedName("weather_code")
            public int weatherCode;

            @SerializedName("surface_pressure")
            public double surfacePressure;
        }
    }

    /**
     * Representa o JSON devolvido pelo BigDataCloud:
     * usamos "city" quando existir, senão "locality", e concatenamos com o
     * "principalSubdivision" (estado).
     */
    public static class GeocodingResponse {
        @SerializedName("city")
        public String city;

        @SerializedName("locality")
        public String locality;

        @SerializedName("principalSubdivision")
        public String principalSubdivision;

        /**
         * Monta o nome amigável da localização.
         *
         * @return "Cidade - Estado", ou só a cidade, ou um texto padrão
         */
        public String buildCityName() {
            String cidade = (city != null && !city.isEmpty()) ? city : locality;
            if (cidade != null && !cidade.isEmpty()) {
                if (principalSubdivision != null && !principalSubdivision.isEmpty()) {
                    return cidade + " - " + principalSubdivision;
                }
                return cidade;
            }
            return "Localização Atual"; // Fallback caso a API não retorne cidade.
        }
    }

    // ------------------------------------------------------------------
    // Instâncias das APIs (criadas uma vez no construtor)
    // ------------------------------------------------------------------

    private final OpenMeteoApi openMeteoApi;
    private final GeocodingApi geocodingApi;

    /**
     * Construtor: monta as instâncias do Retrofit para as duas APIs.
     * Cada Retrofit tem sua própria baseUrl e o mesmo conversor Gson.
     */
    public WeatherRepository() {
        // Retrofit configurado para a API de clima.
        Retrofit meteoRetrofit = new Retrofit.Builder()
                .baseUrl(OPEN_METEO_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        openMeteoApi = meteoRetrofit.create(OpenMeteoApi.class);

        // Retrofit configurado para a API de geocodificação.
        Retrofit geoRetrofit = new Retrofit.Builder()
                .baseUrl(BIG_DATA_CLOUD_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        geocodingApi = geoRetrofit.create(GeocodingApi.class);
    }

    // ------------------------------------------------------------------
    // Método público principal: busca clima + cidade
    // ------------------------------------------------------------------

    /**
     * Busca os dados de clima para uma coordenada e, em seguida, o nome da
     * cidade. Tudo de forma assíncrona (não trava a interface).
     *
     * Fluxo: 1) chama a Open-Meteo; 2) ao receber o clima, chama a BigDataCloud
     * para descobrir a cidade; 3) devolve o WeatherData completo via callback.
     *
     * @param lat      latitude em graus decimais
     * @param lon      longitude em graus decimais
     * @param callback objeto que receberá o sucesso ou o erro
     */
    public void fetchWeather(double lat, double lon, final WeatherCallback callback) {
        // Variáveis "current" solicitadas à API (mesma lista do enunciado).
        String currentParams = "apparent_temperature,relative_humidity_2m,weather_code,surface_pressure";

        Call<WeatherResponse> call =
                openMeteoApi.getCurrentWeather(lat, lon, currentParams, TIMEZONE);

        // enqueue() executa a chamada em uma thread de fundo e devolve o
        // resultado na thread principal — perfeito para atualizar a UI.
        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call,
                                   @NonNull Response<WeatherResponse> response) {
                // Verifica se a resposta HTTP foi bem-sucedida (código 2xx) e tem corpo.
                if (response.isSuccessful() && response.body() != null
                        && response.body().current != null) {

                    WeatherResponse.Current c = response.body().current;

                    // Cria o objeto de modelo com os dados recebidos.
                    final WeatherData data = new WeatherData(
                            c.apparentTemperature,
                            c.relativeHumidity2m,
                            c.weatherCode,
                            c.surfacePressure,
                            null // cityName será preenchido logo abaixo
                    );

                    // Segunda etapa: buscar o nome da cidade.
                    fetchCityName(lat, lon, data, callback);
                } else {
                    // Servidor respondeu, mas com erro ou sem dados esperados.
                    callback.onError("Não foi possível obter os dados do tempo (código "
                            + response.code() + ").");
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                // Erro de rede (sem internet, timeout, etc.).
                callback.onError("Falha de conexão. Verifique sua internet e tente novamente.");
            }
        });
    }

    /**
     * Busca o nome da cidade (geocodificação reversa) e completa o WeatherData.
     * Se a busca da cidade falhar, ainda assim devolvemos o clima com um nome
     * padrão — afinal, o clima é o dado mais importante.
     *
     * @param lat      latitude
     * @param lon      longitude
     * @param data     objeto de clima já preenchido, aguardando o nome da cidade
     * @param callback callback final a ser notificado
     */
    private void fetchCityName(double lat, double lon, final WeatherData data,
                               final WeatherCallback callback) {
        Call<GeocodingResponse> call = geocodingApi.reverseGeocode(lat, lon, "pt");

        call.enqueue(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeocodingResponse> call,
                                   @NonNull Response<GeocodingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setCityName(response.body().buildCityName());
                } else {
                    data.setCityName("Localização Atual");
                }
                // Sucesso final: entrega o modelo completo.
                callback.onSuccess(data);
            }

            @Override
            public void onFailure(@NonNull Call<GeocodingResponse> call, @NonNull Throwable t) {
                // Mesmo sem cidade, devolvemos o clima com o nome padrão.
                data.setCityName("Localização Atual");
                callback.onSuccess(data);
            }
        });
    }
}
