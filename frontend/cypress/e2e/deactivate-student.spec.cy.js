// UC-STU-deactivate-student extension 3a. Run against the dev stack with `npm run cy:open`.

const studentsResponse = {
  flag: true,
  code: 200,
  message: 'Find students successfully',
  data: {
    content: [
      {
        id: 4,
        username: 'j.smith@abc.edu',
        firstName: 'John',
        lastName: 'Smith',
        email: 'j.smith@abc.edu',
        enabled: true,
        teamName: 'Team1'
      }
    ],
    number: 0,
    size: 50,
    totalElements: 1
  }
}

describe('Deactivate student', () => {
  beforeEach(() => {
    cy.login('b.gates@abc.edu', '123456')
    cy.intercept('POST', '**/students/search*', { body: studentsResponse }).as('students')
    cy.intercept('PATCH', '**/students/*/deactivation').as('deactivation')
    cy.visit('/students')
    cy.wait('@students')
  })

  it('does not deactivate the student when the instructor cancels (3a)', () => {
    cy.get('[data-cy=deactivate-open]').click()
    cy.get('[data-cy=deactivate-dialog]').should('be.visible')

    cy.get('[data-cy=deactivate-cancel]').click()

    cy.get('[data-cy=deactivate-dialog]').should('not.be.visible')
    cy.get('@deactivation.all').should('have.length', 0)
  })
})
