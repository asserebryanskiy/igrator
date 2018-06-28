import $ from 'jquery'
import GameSessionUtils from "../../game-session-utils";

export class ProjectorBestPracticeComponent {
    constructor() {
        $.getJSON('/api/conference/bestPractice/' + GameSessionUtils.getGameId(), (data) => {
            for (let practice of data) ProjectorBestPracticeComponent.addBestPractice(practice);
        });

        $('#best-practices-copy-icon').click(() => {
            let allBestPractices = '';
            $('.best-practice-text').each((i, el) => {
                allBestPractices += $(el).text() + '\n';
            });
            navigator.clipboard.writeText(allBestPractices);
            $('#best-practices-text-copied-text')
                .removeClass('hide')
                .show()
                .delay(1000)
                .fadeOut();
        })
    }

    static addBestPractice(practice) {
        const $newPractice = $('.best-practice-template').clone();
        $newPractice.removeClass('hide best-practice-template');
        $newPractice.find('.best-practice-text').text(practice.text);
        $newPractice.find('.best-practice-participant-identifier').text(practice.participantId);
        $newPractice.attr('id', 'best-practice-' + practice.id);
        $('.best-practices-wrapper').prepend($newPractice);
    }

    static changeBestPractice(practice) {
        $('#best-practice-' + practice.id).find('.best-practice-text').text(practice.text);
    }

    static delete(id) {
        $('#best-practice-' + id).remove();
    }
}