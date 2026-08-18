package com.example.weatherapp.controller;

import android.content.Context;

import com.example.weatherapp.model.WeatherData;
import com.example.weatherapp.model.WeatherRepository;

/**
 * WeatherController
 * -----------------
 * Faz o papel de "C" (Controller) no padrão MVC. É o INTERMEDIÁRIO entre o
 * Model (dados/rede) e a View (tela): recebe pedidos da View, aciona o
 * Repository do Model, trata os callbacks e devolve o resultado já pronto para
 * a View exibir.
 *
 * A View NÃO fala diretamente com o Repository — sempre passa pelo Controller.
 * Isso mantém a regra de negócio concentrada aqui e a tela (só exibe).
 *
 * Espelha o WeatherController.php: lá ele instanciava o Model, buscava os dados
 * e repassava para a View; aqui fazemos o mesmo, porém de forma assíncrona.
 *
 * @author Projeto didático WeatherApp
 */
public class WeatherController {

    /** Repositório do Model, responsável pelas chamadas de rede. */
    private final WeatherRepository repository;

    /**
     * Interface que a View (Activity) deve implementar para receber as
     * atualizações. É o "contrato" de comunicação Controller -> View.
     *
     * Definimos aqui (no Controller) para deixar explícito que é o Controller
     * quem dita o que a View precisa saber fazer.
     */
    public interface WeatherView {
        /**
         * Solicita que a View mostre o estado de "carregando" (ProgressBar).
         */
        void showLoading();

        /**
         * Entrega os dados de clima prontos para exibição.
         *
         * @param data objeto WeatherData completo
         */
        void showWeather(WeatherData data);

        /**
         * Informa que ocorreu um erro, para a View exibir uma mensagem amigável.
         *
         * @param message mensagem de erro em português
         */
        void showError(String message);
    }

    /**
     * Construtor.
     *
     * @param context contexto do Android (recebido da Activity). Guardamos a
     *                referência caso seja necessário no futuro (ex.: recursos,
     *                cache). O Repository em si não depende de contexto.
     */
    public WeatherController(Context context) {
        // Cria o repositório uma única vez para reutilizar as instâncias Retrofit.
        this.repository = new WeatherRepository();
    }

    /**
     * Carrega o clima para as coordenadas informadas e repassa o resultado à View.
     *
     * Passo a passo:
     * 1) Avisa a View para mostrar o carregamento;
     * 2) Pede ao Repository para buscar o clima + cidade;
     * 3) Nos callbacks, repassa sucesso ou erro para a View.
     *
     * @param lat  latitude em graus decimais
     * @param lon  longitude em graus decimais
     * @param view a View que exibirá o resultado (geralmente a MainActivity)
     */
    public void loadWeather(double lat, double lon, final WeatherView view) {
        // 1) Estado de carregamento na tela.
        view.showLoading();

        // 2) e 3) Delega para o Model e trata os callbacks.
        repository.fetchWeather(lat, lon, new WeatherRepository.WeatherCallback() {
            @Override
            public void onSuccess(WeatherData data) {
                // Dados chegaram: entrega para a View exibir.
                view.showWeather(data);
            }

            @Override
            public void onError(String message) {
                // Algo falhou: repassa a mensagem amigável para a View.
                view.showError(message);
            }
        });
    }
}
