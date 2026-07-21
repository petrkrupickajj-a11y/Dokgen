// Sekce "Udaje faktury" (cislo faktury, splatnost, tabulka polozek) se
// zobrazuje jen u sablon s konvenci opakovani radku (${polozka.) - kazda
// <option> ve vyberu sablony nese priznak data-ma-polozky (viz generovat.html).
// Fieldset ma navic atribut disabled - dokud je zapnuty, prohlizec jeho pole
// vubec neodesle spolu s formularem, takze se neposilaji zbytecna/matouci
// data pro sablony, ktere polozky vubec nepouzivaji.
(function () {
    var vyberSablony = document.getElementById('sablonaId');
    var sekcePolozky = document.getElementById('sekcePolozky');
    var teloTabulky = document.getElementById('teloTabulkyPolozek');
    var sablonaRadku = document.getElementById('sablonaRadkuPolozky');
    var tlacitkoPridatRadek = document.getElementById('tlacitkoPridatRadek');

    if (!vyberSablony || !sekcePolozky || !teloTabulky || !sablonaRadku || !tlacitkoPridatRadek) {
        return;
    }

    function aktualizujViditelnostSekce() {
        var vybranaMoznost = vyberSablony.options[vyberSablony.selectedIndex];
        var maPolozky = !!vybranaMoznost && vybranaMoznost.getAttribute('data-ma-polozky') === 'true';
        sekcePolozky.hidden = !maPolozky;
        sekcePolozky.disabled = !maPolozky;
    }

    // Spring bindne pole seznamu polozky[N].* jen pri souvislem, od nuly
    // zacinajicim indexovani - proto se po kazdem pridani/odebrani radku
    // indexy vsech zbylych radku prepocitaji odznova.
    function prepocitejIndexyRadku() {
        var radky = teloTabulky.querySelectorAll('.radek-polozky');
        radky.forEach(function (radek, index) {
            radek.querySelector('.pole-nazev').name = 'polozky[' + index + '].nazev';
            radek.querySelector('.pole-mnozstvi').name = 'polozky[' + index + '].mnozstvi';
            radek.querySelector('.pole-cena').name = 'polozky[' + index + '].cena';
        });
    }

    function pridejRadek() {
        var radek = sablonaRadku.content.firstElementChild.cloneNode(true);
        radek.querySelector('.tlacitko-odebrat-radek').addEventListener('click', function () {
            radek.remove();
            prepocitejIndexyRadku();
        });
        teloTabulky.appendChild(radek);
        prepocitejIndexyRadku();
    }

    vyberSablony.addEventListener('change', aktualizujViditelnostSekce);
    tlacitkoPridatRadek.addEventListener('click', pridejRadek);

    aktualizujViditelnostSekce();
    pridejRadek();
})();
