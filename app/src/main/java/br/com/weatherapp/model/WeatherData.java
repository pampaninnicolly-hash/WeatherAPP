package com.example.weatherapp.model;

import java.util.HashMap;
import java.util.Map;

/**
 * WeatherData
 * ------------
 * POJO (Plain Old Java Object) que representa, de forma organizada, os dados de
 * clima que a aplicação exibe na tela. Faz o papel de "M" (Model) no padrão MVC:
 * é um objeto simples de transporte de dados, sem regra de tela.
 *
 * Espelha os campos usados no projeto PHP de referência (temperatura aparente,
 * umidade, código do tempo e pressão) e adiciona o nome da cidade e a descrição
 * textual do tempo (mapeada a partir do weatherCode).
 *
 * @author Projeto didático WeatherApp
 */
public class WeatherData {

    // ------------------------------------------------------------------
    // Campos (atributos) do modelo
    // ------------------------------------------------------------------

    /** Temperatura aparente (sensação térmica), em °C. Campo apparent_temperature da API. */
    private double apparentTemperature;

    /** Umidade relativa do ar a 2 metros, em %. Campo relative_humidity_2m da API. */
    private int relativeHumidity2m;

    /** Código WMO do tempo (0=céu limpo, 3=nublado, etc.). Campo weather_code da API. */
    private int weatherCode;

    /** Pressão na superfície, em hPa. Campo surface_pressure da API. */
    private double surfacePressure;

    /** Nome da cidade obtido por geocodificação reversa (BigDataCloud). */
    private String cityName;

    /** Descrição textual do tempo (com emoji), derivada do weatherCode. */
    private String weatherDescription;

    // ------------------------------------------------------------------
    // Construtores
    // ------------------------------------------------------------------

    /** Construtor vazio — útil para o Gson e para preenchimento incremental. */
    public WeatherData() {
    }

    /**
     * Construtor completo.
     *
     * @param apparentTemperature sensação térmica em °C
     * @param relativeHumidity2m  umidade relativa em %
     * @param weatherCode         código WMO do tempo
     * @param surfacePressure     pressão em hPa
     * @param cityName            nome da cidade
     */
    public WeatherData(double apparentTemperature, int relativeHumidity2m,
                       int weatherCode, double surfacePressure, String cityName) {
        this.apparentTemperature = apparentTemperature;
        this.relativeHumidity2m = relativeHumidity2m;
        this.weatherCode = weatherCode;
        this.surfacePressure = surfacePressure;
        this.cityName = cityName;
        // Já preenche a descrição a partir do código informado.
        this.weatherDescription = getDescription(weatherCode);
    }

    // ------------------------------------------------------------------
    // Mapa de códigos WMO -> descrição em português (igual ao PHP)
    // ------------------------------------------------------------------

    /**
     * Mapa estático (criado uma única vez) que traduz o código WMO retornado
     * pela API para uma descrição amigável em português, com emoji.
     * É "static final" porque é compartilhado por todas as instâncias e não muda.
     */
    private static final Map<Integer, String> WMO_CODES = new HashMap<>();

    // Bloco estático: executado uma vez quando a classe é carregada na memória.
    static {
        WMO_CODES.put(0,  "Céu limpo ☀️");
        WMO_CODES.put(1,  "Quase limpo 🌤️");
        WMO_CODES.put(2,  "Parcialmente nublado ⛅");
        WMO_CODES.put(3,  "Nublado ☁️");
        WMO_CODES.put(45, "Nevoeiro 🌫️");
        WMO_CODES.put(48, "Nevoeiro com geada 🌫️");
        WMO_CODES.put(51, "Garoa leve 🌧️");
        WMO_CODES.put(53, "Garoa moderada 🌧️");
        WMO_CODES.put(55, "Garoa densa 🌧️");
        WMO_CODES.put(61, "Chuva leve ☔");
        WMO_CODES.put(63, "Chuva moderada ☔");
        WMO_CODES.put(65, "Chuva forte ☔");
        WMO_CODES.put(71, "Neve leve ❄️");
        WMO_CODES.put(73, "Neve moderada ❄️");
        WMO_CODES.put(75, "Neve forte ❄️");
        WMO_CODES.put(95, "Tempestade ⛈️");
        WMO_CODES.put(96, "Tempestade com granizo leve ⛈️");
        WMO_CODES.put(99, "Tempestade com granizo forte ⛈️");
    }

    /**
     * Traduz um código WMO em uma descrição legível em português.
     * Equivale ao método getWeatherDescription() do WeatherModel.php.
     *
     * @param code código WMO do tempo
     * @return descrição amigável com emoji, ou um texto padrão se o código
     *         não estiver mapeado
     */
    public static String getDescription(int code) {
        // getOrDefault: retorna o valor do mapa ou o padrão caso a chave não exista.
        return WMO_CODES.getOrDefault(code, "Condição desconhecida");
    }

    // ------------------------------------------------------------------
    // Getters e Setters (encapsulamento dos atributos)
    // ------------------------------------------------------------------

    public double getApparentTemperature() {
        return apparentTemperature;
    }

    public void setApparentTemperature(double apparentTemperature) {
        this.apparentTemperature = apparentTemperature;
    }

    public int getRelativeHumidity2m() {
        return relativeHumidity2m;
    }

    public void setRelativeHumidity2m(int relativeHumidity2m) {
        this.relativeHumidity2m = relativeHumidity2m;
    }

    public int getWeatherCode() {
        return weatherCode;
    }

    /**
     * Ao definir o weatherCode também atualizamos automaticamente a descrição,
     * mantendo os dois campos sempre coerentes.
     *
     * @param weatherCode código WMO do tempo
     */
    public void setWeatherCode(int weatherCode) {
        this.weatherCode = weatherCode;
        this.weatherDescription = getDescription(weatherCode);
    }

    public double getSurfacePressure() {
        return surfacePressure;
    }

    public void setSurfacePressure(double surfacePressure) {
        this.surfacePressure = surfacePressure;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getWeatherDescription() {
        // Se ainda não foi calculada, deriva do código no momento da leitura.
        if (weatherDescription == null) {
            weatherDescription = getDescription(weatherCode);
        }
        return weatherDescription;
    }

    public void setWeatherDescription(String weatherDescription) {
        this.weatherDescription = weatherDescription;
    }
}
