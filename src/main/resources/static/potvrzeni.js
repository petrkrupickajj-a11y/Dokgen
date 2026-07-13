// Vlastni potvrzovaci modal namisto nativniho window.confirm() - na rozdil od nej
// nezablokuje renderovaci proces stranky (ktery nativni confirm() pozastavi cele
// a zamezi tak napr. automatizovanemu ovladani prohlizece), jen prekresli DOM.
// Kazdy formular, ktery ma potvrzeni vyzadovat, dostane atribut data-potvrzeni
// s hotovou (uz v Thymeleaf sablone zformatovanou) zpravou.
(function () {
    var modal = null;

    function vytvorModal() {
        var prekryti = document.createElement('div');
        prekryti.className = 'modal-prekryti';
        prekryti.hidden = true;
        prekryti.innerHTML =
            '<div class="modal-okno" role="alertdialog" aria-modal="true" aria-labelledby="modal-text">' +
            '<p id="modal-text"></p>' +
            '<div class="modal-akce">' +
            '<button type="button" class="zrusit"></button>' +
            '<button type="button" class="potvrdit"></button>' +
            '</div></div>';
        document.body.appendChild(prekryti);
        return prekryti;
    }

    function potvrd(zprava) {
        if (!modal) {
            modal = vytvorModal();
        }
        var text = modal.querySelector('#modal-text');
        var tlacitkoZrusit = modal.querySelector('.zrusit');
        var tlacitkoPotvrdit = modal.querySelector('.potvrdit');
        text.textContent = zprava;
        tlacitkoZrusit.textContent = document.body.getAttribute('data-tlacitko-zrusit');
        tlacitkoPotvrdit.textContent = document.body.getAttribute('data-tlacitko-potvrdit');

        return new Promise(function (resolve) {
            modal.hidden = false;
            tlacitkoPotvrdit.focus();

            function zavri(vysledek) {
                modal.hidden = true;
                tlacitkoPotvrdit.removeEventListener('click', naPotvrdit);
                tlacitkoZrusit.removeEventListener('click', naZrusit);
                modal.removeEventListener('click', naKlikPrekryti);
                document.removeEventListener('keydown', naKlaves);
                resolve(vysledek);
            }
            function naPotvrdit() { zavri(true); }
            function naZrusit() { zavri(false); }
            function naKlikPrekryti(event) { if (event.target === modal) zavri(false); }
            function naKlaves(event) { if (event.key === 'Escape') zavri(false); }

            tlacitkoPotvrdit.addEventListener('click', naPotvrdit);
            tlacitkoZrusit.addEventListener('click', naZrusit);
            modal.addEventListener('click', naKlikPrekryti);
            document.addEventListener('keydown', naKlaves);
        });
    }

    document.querySelectorAll('form[data-potvrzeni]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            potvrd(form.getAttribute('data-potvrzeni')).then(function (potvrzeno) {
                if (potvrzeno) {
                    form.submit();
                }
            });
        });
    });
})();
